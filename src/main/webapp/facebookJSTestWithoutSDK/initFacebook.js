        config={
          clientId: '317629754933360',
          redirectUri: 'http://server.local.network.cz:8080/facebookTest/facebookJSTestWithoutSDK/facebook.html'
        };

        var hashh = window.location.hash;
        var accessToken = false;
        if (hashh) {
           hashh = hashh.substring(1);
           ind1 = hashh.indexOf('=') + 1;
           ind2 = hashh.indexOf('&');
           accessToken = hashh.substring(ind1, ind2);
        } else {
           // initFacebookFlow();
        }

        function initFacebookFlow() {
          var fbUrl = 'https://www.facebook.com/dialog/oauth?'
          + 'client_id=' + config.clientId
          + '&redirect_uri=' + config.redirectUri
          + '&response_type=token'
          + '&scope=email';

          window.location = fbUrl;
        }

        if (accessToken) {
           sendAjaxRequest("https://graph.facebook.com/me?access_token=" + accessToken, function(status, jsonResponse) {
               if (status == 200) {
                 document.getElementById('myusername').innerHTML=response.username;
                 document.getElementById('myfn').innerHTML=response.first_name;
                 document.getElementById('myln').innerHTML=response.last_name;
                 document.getElementById('myemail').innerHTML=response.email;
               } else {
                 document.getElementById('error').innerHTML = response;
               }
           });
        }

        function sendAjaxRequest(uri, func) {
                      var xmlhttp;
                      if (window.XMLHttpRequest)
                      {// code for IE7+, Firefox, Chrome, Opera, Safari
                         xmlhttp=new XMLHttpRequest();
                      }
                      else
                      {// code for IE6, IE5
                        xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
                      }

                      xmlhttp.onreadystatechange=function()
                      {
                        if (xmlhttp.readyState==4 && xmlhttp.status==200)
                        {
                          response = JSON.parse(xmlhttp.responseText);
                          func(xmlhttp.status, response);
                        }
                      }
                      xmlhttp.open("GET", uri, true);
                      xmlhttp.send();
        }