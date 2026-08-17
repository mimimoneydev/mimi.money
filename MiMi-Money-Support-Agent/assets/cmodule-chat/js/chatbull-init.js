var ptitle = document.title;
var purl = window.location.href;
var iframeurl = cburl + 'index.php?d=visitors&c=chatbox&m=chatpanel&token=' + access_token;
var is_mobile = false;
var chat_cors_header = true;
var app_environment = 'production'; // may be production/development
var minify_str = (app_environment === 'production') ? '.min' : '';

var cbwindow = {
    is_mobile: window.is_mobile,
    innerHeight: window.innerHeight,
    outerHeight: window.outerHeight,
    innerWidth: window.innerWidth,
    outerWidth: window.outerWidth
};

/*
 * To chnage object to quesry string.
 */
function buildQueryParam(obj) {
    var query = '', name, value;

    for (name in obj) {
        value = obj[name];
        query += encodeURIComponent(name) + '=' + encodeURIComponent(value) + '&';
    }

    return query.length ? query.substr(0, query.length - 1) : query;
}

/*
 * To check is mobile window or not.
 */
function detectmob() {
    if (navigator.userAgent.match(/Android/i)
            || navigator.userAgent.match(/webOS/i)
            || navigator.userAgent.match(/iPhone/i)
            || navigator.userAgent.match(/iPad/i)
            || navigator.userAgent.match(/iPod/i)
            || navigator.userAgent.match(/BlackBerry/i)
            || navigator.userAgent.match(/Windows Phone/i)
            ) {
        return true;
    } else {
        return false;
    }
}
is_mobile = detectmob();

window.onload = function() {
    // loading stylesheet for chat iframe
    var stylesheet_init = document.createElement("link"); // Make a script DOM node
    stylesheet_init.href = cburl + 'assets/cmodule-chat/css/chatbox-widget' + minify_str + '.css'; // Set it's src to the provided URL
    stylesheet_init.rel = 'stylesheet';
    document.head.appendChild(stylesheet_init);

    if (is_mobile) {
        iframeurl = cburl + 'index.php?d=visitors&c=chatbox&m=button&token=' + access_token;
    }

    if (typeof cbuser !== 'undefined') {
        iframeurl += '&' + buildQueryParam(cbuser);
    }

    cbwindow.is_mobile = is_mobile;
    iframeurl += '&' + buildQueryParam(cbwindow);

    // creating iframe to load chatbox
    var iframe_chat = document.createElement('iframe');
    iframe_chat.style.position = "fixed";
    iframe_chat.style.height = "0";
    iframe_chat.style.width = "0";
    iframe_chat.scrolling = "no";
    iframe_chat.name = ptitle + '[!]' + purl;
    iframe_chat.id = 'chatbull-frame';
    iframe_chat.className = 'chat-cmodule-iframe';
    iframe_chat.src = iframeurl;
    document.body.appendChild(iframe_chat);

    // loading chat scripts
    if (typeof chat_cors_header !== 'undefined' && chat_cors_header) {
        var script_init = document.createElement("script"); // Make a script DOM node
        script_init.src = cburl + 'assets/cmodule-chat/js/chatbox-init' + minify_str + '.js'; // Set it's src to the provided URL
        script_init.type = 'text/javascript';
        document.body.appendChild(script_init);
    } else {
        var script_setting = document.createElement("script"); // Make a script DOM node
        script_setting.src = cburl + 'index.php?d=visitors&c=chatbox&m=settings&token=' + access_token; // Set it's src to the provided URL
        script_setting.type = 'text/javascript';
        document.body.appendChild(script_setting);

        var script_init = document.createElement("script"); // Make a script DOM node
        script_init.src = cburl + 'assets/cmodule-chat/js/chatbox-init2' + minify_str + '.js'; // Set it's src to the provided URL
        script_init.type = 'text/javascript';
        document.body.appendChild(script_init);
    }
}