angular.module('cmodule.filters', []).
        filter('checkmark', function () {
            return function (input) {
                return input ? '\u2713' : '\u2718';
            };
        })
        .filter('status', function () {
            return function (input, status) {
                return input == status ? '\u2713' : '\u2718';
            };
        })
        .filter('statusClass', function () {
            return function (input, status) {
                return input == status ? 'on' : 'off';
            };
        })
        .filter('capitalize', function () {
            return function (input) {
                return input.toLowerCase().replace(/\b[a-z]/g, function (letter) {
                    return letter.toUpperCase();
                });
            };
        })
        .filter('oneCapLetter', function () {
            return function (input) {
                return input.substring(0, 1).toLowerCase().replace(/\b[a-z]/g, function (letter) {
                    return letter.toUpperCase();
                });
            };
        })
        .filter('htmlToPlaintext', function () {
            return function (text) {
                return  text ? String(text).replace(/<[^>]+>/gm, '') : '';
            };
        })
        .filter('cut', function () {
            return function (value, wordwise, max, tail) {
                if (!value)
                    return '';

                max = parseInt(max, 10);
                if (!max)
                    return value;
                if (value.length <= max)
                    return value;

                value = value.substr(0, max);
                if (wordwise) {
                    var lastspace = value.lastIndexOf(' ');
                    if (lastspace != -1) {
                        value = value.substr(0, lastspace);
                    }
                }

                return value + (tail || ' …');
            };
        })
        .filter('dateToTimestamp', function () {
            return function (inputDate) {
                var dateParts = inputDate.split('-');

                var today = new Date();
                var timeshift = today.getTimezoneOffset();
                var inputdate = new Date(dateParts[0], parseInt(dateParts[1], 10) - 1, dateParts[2]);

                if (timeshift < 0) {
                    inputdate.setMinutes(inputdate.getMinutes() + Math.abs(timeshift));
                } else if (timeshift > 0) {
                    inputdate.setMinutes(inputdate.getMinutes() - Math.abs(timeshift));
                }

                return inputdate.getTime();
            };
        })
        .filter('datetimeToTimestamp', function () {
            return function (inputDate) {
                var dateTimeParts = inputDate.split(' '),
                        timeParts = dateTimeParts[1].split(':'),
                        dateParts = dateTimeParts[0].split('-');

                var today = new Date();
                var timeshift = today.getTimezoneOffset();
                var inputdate = new Date(dateParts[0], parseInt(dateParts[1], 10) - 1, dateParts[2], timeParts[0], timeParts[1]);

                if (timeshift < 0) {
                    inputdate.setMinutes(inputdate.getMinutes() + Math.abs(timeshift));
                } else if (timeshift > 0) {
                    inputdate.setMinutes(inputdate.getMinutes() - Math.abs(timeshift));
                }

                return inputdate.getTime();
            };
        })
        .filter('newlines', function () {
            var ishtml = function (str) {
                var a = document.createElement('div');
                a.innerHTML = str;
                for (var c = a.childNodes, i = c.length; i--; ) {
                    if (c[i].nodeType == 1)
                        return true;
                }
                return false;
            };

            return function (text) {
                if (text) {
                    if (ishtml(text)) {
                        return text.replace(/\n/g, ' <br/> ')
                                .replace(/([A-Za-z0-9._%+-]+@+[A-Za-z0-9._%+-]+\.[^\s]+)/g, '<a target="_blank" href="mailto:$1">$1</a>');
                    }

                    return text.replace(/\n/g, ' <br/> ')
                            .replace(/((ftp|http)[^\s]+)/g, '<a target="_blank" href="$1">$1</a>')
                            //.replace(/(www\.[^\s]+)/g, 'http://$1')
                            //.replace(/(www\.[^\s]+)/g, '<a target="_blank" href="http://$1">$1</a>')
                            .replace(/([A-Za-z0-9._%+-]+@+[A-Za-z0-9._%+-]+\.[^\s]+)/g, '<a target="_blank" href="mailto:$1">$1</a>');
                }

                return text;
            };
        })
        .filter('math_round', function () {
            return function (inputNumber) {
                return  inputNumber ? Math.round(inputNumber) : '';
            };
        })
        .filter('str_human', function () {
            return function (text) {
                return  text ? text.replace('_', ' ') : text;
            };
        })
        .filter('prep_url', function () {
            return function (siteurl) {
                if(siteurl) {
                    var prefix = 'http://';
					var prefix2 = 'https://';
                    if(siteurl.substr(0, prefix.length) !== prefix && siteurl.substr(0, prefix2.length) !== prefix2) {
                        return prefix + siteurl;
                    }
                }
                
                return siteurl;
            };
        })
        .filter('do_seprate', function () {
            return function (str, seprator) {
                return  str ? str+seprator : str;
            };
        });

angular.module('cmodule.services', []).
        factory('alertService', function () {
            return {
                showMessage: false,
                message: '',
                showErrors: false,
                errors: ''
            };
        }).
        factory('filterService', function () {
            return {
                keywords: '',
                roles: [],
                agents: [],
                tags: [],
                offset: 0
            };
        });

