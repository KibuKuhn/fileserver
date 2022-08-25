var downloadPath = "";
var fileName = "";

const Direction = {
    up : 1,
    down : 2
}

function onPageLoad() { //on page load 
    //buttons
    var download = $("#download");
    download.attr("disabled", true);
    download.click(downloadFile);
    //    var upload = $("#upload");
    //    upload.click(uploadFile);

    // Create the tree inside the <div id="tree"> element.
    $("#tree").fancytree({
        //extensions : [ "edit", "filter" ],
        source: {
            cache: false,
            url: "/data/getNodes"
        },
        lazyLoad: function(event, data) {
            console.log("layzyLoad: event=" + event + ", data=" + data);
            var node = data.node;
            // Issue an Ajax request to load child nodes
            data.result = {
                url: "/data/getTreeData",
                data: {
                    mode: "children",
                    path: encodeURI(node.data.path)
                }
            }
        },
        activate: function(event, data) {
            // A node was activated (selected):
            resetLabel(Direction.down);
            var node = data.node;
            console.log("echo Active: " + node.title);
            $("#download").attr("disabled", node.isFolder());
            if (node.isFolder()) {
                return;
            }

            downloadPath = node.data.path;
            fileName = node.data.fileName;
        }
    });
    // Note: Loading and initialization may be asynchronous, so the nodes may not be accessible yet.
};

function ajaxTransport() {
    /**
     *
     * jquery.binarytransport.js
     *
     * @description. jQuery ajax transport for making binary data type requests.
     * @version 1.0 
     * @author Henry Algus <henryalgus@gmail.com>
     *
     */

    // use this transport for "binary" data type
    $.ajaxTransport(
        "+binary",
        function(options, originalOptions, jqXHR) {
            // check for conditions and support for blob / arraybuffer response type
            if (window.FormData
                && ((options.dataType && (options.dataType == 'binary')) || (options.data && ((window.ArrayBuffer && options.data instanceof ArrayBuffer) || (window.Blob && options.data instanceof Blob))))) {
                return {
                    // create new XMLHttpRequest
                    send: function(headers, callback) {
                        // setup all variables
                        var xhr = new XMLHttpRequest(),
                            url = options.url,
                            type = options.type,
                            async = options.async || true,
                            // blob or arraybuffer. Default is blob
                            dataType = options.responseType
                                || "blob", data = options.data
                                    || null, username = options.username
                                        || null, password = options.password
                                            || null;

                        xhr.addEventListener(
                            'load',
                            function() {
                                var data = {};
                                data[options.dataType] = xhr.response;
                                // make callback and send data
                                callback(
                                    xhr.status,
                                    xhr.statusText,
                                    data,
                                    xhr
                                        .getAllResponseHeaders());
                            });

                        xhr.open(type, url, async, username,
                            password);

                        // setup custom headers
                        for (var i in headers) {
                            xhr.setRequestHeader(i, headers[i]);
                        }

                        xhr.responseType = dataType;
                        xhr.send(data);
                    },
                    abort: function() {
                        jqXHR.abort();
                    }
                };
            }
        });
};

function downloadFile() {
    ajaxTransport();
    if (!downloadPath || !fileName) {
        return;
    }

    var encoded = downloadPath;
    var json = JSON.stringify({
        uri: encoded
    });
    $.ajax({
        url: "/data/download",
        type: "POST",
        contentType: "application/json; charset=utf-8",
        data: json,
        cache: false,
        dataType: "binary",
        processData: false,
        success: function(data) {
            resetLabel(Direction.down);
            var blob = new Blob([data]);
            var link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download = fileName;
            link.click();
        },
        error: function(xhr, status, exception) {
            setError(status + ": " + exception, Direction.down);
        }
    });
};

function getInfoLabel(direction) {
    if (direction === Direction.up) {
        return $("#uploadLabel");
    } else if (direction === Direction.down) {
        return $("#downloadLabel");
    } else {
        throw new Error("Undefined deirection");
    }
};

function setError(text, direction) {
    var label = getInfoLabel(direction);
    label.css("color", "red");
    label.text(text);
};

function setInfo(text, direction) {
    var label = getInfoLabel(direction);
    label.css("color", "green");
    label.text(text);
};

function resetLabel(direction) {
    let label = getInfoLabel(direction);
    label.text(null);
};

async function uploadFile() {
    resetLabel(Direction.up);
    let formData = new FormData();
    let file = $("#file")[0].files[0];
    formData.append("file", file);
    fetch('/data/upload', {
        method: "POST",
        body: formData
    })
        .then(data => onUploadSuccess(data))
        .catch(error => onUploadError(error));
};

function onUploadSuccess(response) {
    console.log("Success: ", response);
    if (response.ok) {
        setInfo("File uploaded", Direction.up);
    } else {
        readErrorResponse(response);
    }
};

function readErrorResponse(response) {
    const reader = response.body.getReader();
    let charsReceived = "";
    reader.read().then(function processText(data) {
        if (data.done) {//Unit8Array is data.value
            var obj;
            try {
                obj = JSON.parse(charsReceived);
            } catch (error) {
                //
            }
            if (obj != undefined && obj.message) {
                charsReceived = obj.message;
                onUploadError("Error: " + charsReceived);
            } else {
                onUploadError("Error: Status: " + response.status + ", " + response.statusText);
            }
            return;
        }
        charsReceived += Array.from(data.value).map(c => String.fromCharCode(c)).join('');
        return reader.read().then(processText);
    })
};

function onUploadError(error) {
    setError(error, Direction.up);
};

