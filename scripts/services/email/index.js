/* 
 * Copyright 2021 SolutionX Software Sdn Bhd &lt;info@solutionx.com.my&gt;.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
  
function email() {
}

email.prototype = {
  _setup(serviceName, args, system, path, ctx) {
    this._loggername = "services." + serviceName;
    log.info(this, "Module Name: {}, my path is: {}, args: {}", serviceName, path, args);
    this.profiles = args.profiles;
  },
  getLoggerName() {
    return this._loggername;
  },
  send(args, ctx) {
    var profile = this.profiles[args.profile || 'default'];
    switch (profile.type) {
      case 'alicloud':
        return this.sendAlicloud(args, ctx);
      default:
        throw new Error("Invalid email profile / profile type");
    }
  },
  sendAlicloud(args, ctx) {
  	var profile = this.profiles[args.profile || 'default'];
  	if (profile == null)
  		throw new Error("Invalid Email Profile: " + args.profile);

  	var now = dayjs.utc();
    var date_str = now.format("ddd, DD MMM YYYY HH:mm:ss [GMT]")
  	var iso_date = now.format('YYYY-MM-DD[T]HH:mm:ss[+00:00Z]');
  	var UUID = Java.type('java.util.UUID');
  	var host = "dm.ap-southeast-1.aliyuncs.com";
  	url = `https://${host}/`;
  	var parameters = {};
    parameters["Format"] = "json"
    parameters["AccessKeyId"] = profile.key
    parameters["SignatureMethod"] = "HMAC-SHA1"
    parameters["SignatureType"] = ""
    parameters["SignatureVersion"] = "1.0"
    // parameters["SignatureNonce"] = "c443face-aa99-44ec-98ac-e91bcc11a7fc";
    parameters["SignatureNonce"] = UUID.randomUUID();
    parameters["Timestamp"] = iso_date
    // parameters["Version"] = "2015-11-23"
    parameters["Version"] = "2017-06-22"
    parameters["RegionId"] = "ap-southeast-1"
    parameters["Action"] = "SingleSendMail"
    parameters["AddressType"] = "1"
    parameters["ReplyToAddress"] = "true"
    parameters["AccountName"] = profile.account
    parameters["FromAlias"] = profile.alias
    parameters["ToAddress"] = args.to
    parameters["Subject"] = args.subject
    if (args.html)
        parameters["HtmlBody"] = args.html

    if (args.text)
        parameters["TextBody"] = args.text

    var calc_str = "";
    Object.keys(parameters).sort().forEach(
  	  (key) => { 
  	  	calc_str += encodeURIComponent(key) + "=" + encodeURIComponent(parameters[key]) + "&";
  	  }, 
  	  {}
	  );
	  calc_str = calc_str.substring(0, calc_str.length - 1);
    calc_str = calc_str.replace(/\+/g, '%2B');
    calc_str = calc_str.replace(/\*/g, '%2A');
    calc_str = calc_str.replace(/\%7E/g, '~');
    calc_str = calc_str.replace(/\%/g, '%25');
    calc_str = calc_str.replace(/&/g, '%26');
    calc_str = calc_str.replace(/=/g, '%3D');
/*
    calc_str = calc_str.replace(/\%40/g, '%2540');
    calc_str = calc_str.replace(/\%3C/g, '%253C');
    calc_str = calc_str.replace(/\%3E/g, '%253E');
    calc_str = calc_str.replace(/\%20/g, '%2520');
    calc_str = calc_str.replace(/\%3A/g, '%253A');
    calc_str = calc_str.replace(/\%2F/g, '%252F');
    calc_str = calc_str.replace(/\%2C/g, '%252C');
    calc_str = calc_str.replace(/\%2B/g, '%252B');
*/
    // calc_str = unescape(calc_str);
/*
    calc_str = calc_str.replace(/\+/g, '%252B');
*/
//    calc_str = calc_str.replace(/\*/g, '%2A');
/*
    calc_str = calc_str.replace(/\%7E/g, '~');
    calc_str = calc_str.replace(/\=/g, '%3D');
    calc_str = calc_str.replace(/\&/g, '%26');
    calc_str = calc_str.replace(/@/g, '%2540');
    calc_str = calc_str.replace(/\ /g, '%2520');
    calc_str = calc_str.replace(/,/g, '%252C');
    calc_str = calc_str.replace(/:/g, '%253A');
    calc_str = calc_str.replace(/\[/g, '%255B%2527');
    calc_str = calc_str.replace(/\]/g, '%2527%255D');
    calc_str = calc_str.replace(/\%2B/g, '%252B');
    calc_str = calc_str.replace(/\</g, "%253C");
    calc_str = calc_str.replace(/\>/g, "%253E");
*/
    calc_str = "POST&%2F&" + calc_str;

    const HmacUtils = Java.type("org.apache.commons.codec.digest.HmacUtils");
    const HmacAlgorithms = Java.type("org.apache.commons.codec.digest.HmacAlgorithms");
  	var signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, profile.secret + "&").hmac(calc_str);
    const Base64 = Java.type("org.apache.commons.codec.binary.Base64");
    signature = Base64.encodeBase64String(signature);
	  parameters["Signature"] = signature;

    // console.log(calc_str);
    // console.log(signature);
    var ret = ctx.service("requests").post({
      url: url,
      data: parameters,
      headers: {
	    'Date': date_str,
	    'Host': host
      }
    }, ctx);
    return ret;
  }
};

return email;

}());