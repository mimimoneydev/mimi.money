(function () {
    var controlle_name = 'page-controller';
    modules.push(controlle_name);
    angular.module(controlle_name, []).controller("DashboardController", function ($scope, $http, $timeout, Setting, $log) {
        // object to hold all the data for the dashboard data
        $scope.sites = [];
        $scope.current_site = {id: 0, site_name: ''};
        $scope.pageviews = [];
        $scope.geo_markers = [];
        $scope.filter_by = 'all';
        $scope.online_requests = {total: 0};
        $scope.offline_requests = {total: 0};
        $scope.most_rated_operators = [];
        $scope.recent_chats = [];
        $scope.operators_n_requests = [];

        $scope.monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
        $scope.working_years = cmodule.working_years;
        $scope.current_year = angular.copy(cmodule.current_year);

        var currentDate = new Date();
        $scope.current_month = {name: $scope.monthNames[currentDate.getMonth()], month: currentDate.getMonth() + 1}

        $http.get(Setting.site_url + "?c=admin&m=get_dashboard_data&site_id=" + $scope.current_site.id + '&year=' + $scope.current_year.year + '&month=' + $scope.current_month.month).success(function (response) {
            if (response.result == 'success') {
                $scope.pageviews = response.pageviews;
                $scope.online_requests = response.online_requests;
                $scope.offline_requests = response.offline_requests;
                $scope.most_rated_operators = response.most_rated_operators;
                $scope.recent_chats = response.recent_chats;
                $scope.operators_n_requests = response.operators_n_requests;

                $scope.loadFlotChart(response);
                if (Setting.settings.google_map_api_key) {
                    $scope.drawMap(response);
                }
            }
        });

        /*
         * To draw map on dashboard
         * 
         * @param Object response
         * @returns {undefined}
         */
        $scope.drawMap = function (response) {
            var marker_visitor = Setting.theme_url + 'images/user-location-blue.png';
            var marker_anonymous = Setting.theme_url + 'images/user-location-yellow.png';

            var latlng = new google.maps.LatLng(21.0000, 78.0000);
            var myOptions = {
                zoom: parseInt(Setting.settings.google_map_zoom),
                center: latlng,
                mapTypeControl: true,
                mapTypeControlOptions: {style: google.maps.MapTypeControlStyle.DROPDOWN_MENU},
                navigationControl: true,
                mapTypeId: google.maps.MapTypeId.ROADMAP
            };
            var map = new google.maps.Map(document.getElementById("usersMap"), myOptions);
            map.setOptions({styles: [
                    {
                        elementType: 'geometry',
                    },
                    {
                        featureType: 'all',
                        elementType: 'labels',
                        stylers: [
                            {visibility: 'on'}
                        ]
                    },
                    {
                        elementType: 'labels.icon',
                        stylers: [{visibility: 'on'}]
                    },
                    {
                        elementType: 'labels.text.fill',
                        stylers: [{color: '#616161'}]
                    },
                    {
                        elementType: 'labels.text.stroke',
                        stylers: [{color: '#f5f5f5'}]
                    },
                    {
                        featureType: 'administrative.land_parcel',
                        elementType: 'labels.text.fill'
                    },
                    {
                        featureType: 'poi',
                        elementType: 'geometry'
                    },
                    {
                        featureType: 'poi',
                        elementType: 'labels.text.fill'
                    },
                    {
                        featureType: 'poi.park',
                        elementType: 'geometry'
                    },
                    {
                        featureType: 'poi.park',
                        elementType: 'labels.text.fill'
                    },
                    {
                        featureType: 'road',
                        elementType: 'geometry'
                    },
                    {
                        featureType: 'road.arterial',
                        elementType: 'labels.text.fill'
                    },
                    {
                        featureType: 'road.highway',
                        elementType: 'geometry'
                    },
                    {
                        featureType: 'road.highway',
                        elementType: 'labels.text.fill',
                        stylers: [{color: '#44b5ef'}]
                    },
                    {
                        featureType: 'road.local',
                        elementType: 'labels.text.fill',
                        stylers: [{color: '#44b5ef'}]
                    },
                    {
                        featureType: 'transit.line',
                        elementType: 'geometry',
                        stylers: [{color: '#44b5ef'}]
                    },
                    {
                        featureType: 'transit.station',
                        elementType: 'geometry',
                        stylers: [{color: '#44b5ef'}]
                    },
                    {
                        featureType: 'water',
                        elementType: 'geometry',
                        stylers: [{color: '#ffffff'}]
                    },
                    {
                        featureType: 'water',
                        elementType: 'labels.text.fill',
                        stylers: [{color: '#44b5ef'}]
                    }
                ]});

            google.maps.event.addDomListenerOnce(map, 'idle', function () {
                var center = map.getCenter();
                google.maps.event.addDomListener(window, 'resize', function () {
                    map.setCenter(center);
                });
            });

            angular.forEach(response.visitors, function (addRess, key) {
                addRess.filter_by = 'attended_visitors';

                var infoContent = '<div class="map-location-view">';
                infoContent += '<div class="location-thumb">';

                if (addRess.profile_pic) {
                    infoContent += '<img title="' + addRess.name + '" src=' + addRess.userProfilePic + ' alt=' + addRess.name + ' width=50 height=50 />';
                } else {
                    infoContent += '<span style="background-color: ' + addRess.profile_color + ';" class="user-avatar" title="' + addRess.name + '">' + $scope.oneCapLetter(addRess.name) + '</span>';
                }

                infoContent += '</div>';
                infoContent += '<div class="location-details">';
                infoContent += '<p>' + addRess.name + '</p>';
                infoContent += '<address>' + (addRess.city) + ', ' + (addRess.state) + ', ' + (addRess.country) + '</address>';
                infoContent += '</div>';
                infoContent += '</div>';

                addRess.filter_by = 'attended_visitors';
                addRess.marker_icon = marker_visitor;
                addRess.info_content = infoContent;

                var marker = $scope.createMarker(addRess, map);
                $scope.geo_markers.push(marker);
            });

            angular.forEach(response.anonymous_users, function (addRess, key) {
                var infoContent = '<div class="map-location-view">';
                infoContent += '<div class="location-thumb">';

                infoContent += '<span style="background-color: ' + addRess.profile_color + '" class="user-avatar" title="' + addRess.name + '">' + $scope.oneCapLetter(addRess.name) + '</span>';

                infoContent += '</div>';
                infoContent += '<div class="location-details">';
                infoContent += '<p>' + addRess.name + '</p>';
                infoContent += '<address>' + (addRess.city) + ', ' + (addRess.state) + ', ' + (addRess.country) + '</address>';
                infoContent += '</div>';
                infoContent += '</div>';

                addRess.filter_by = 'online_visitors';
                addRess.marker_icon = marker_anonymous;
                addRess.info_content = infoContent;

                var marker = $scope.createMarker(addRess, map);
                $scope.geo_markers.push(marker);
            });
        }

        /*
         * Filter map markers
         * 
         * @param Event event
         * @param String filter_by
         * @returns {undefined}
         */
        $scope.filter_map = function (event, filter_by) {
            event.preventDefault();
            $scope.filter_by = filter_by;

            for (i = 0; i < $scope.geo_markers.length; i++) {
                var gmarker = $scope.geo_markers[i];
                // If is same category or category not picked
                if (gmarker.category == filter_by || filter_by == 'all') {
                    gmarker.setVisible(true);
                }
                // Categories don't match 
                else {
                    gmarker.setVisible(false);
                }
            }
        }

        /*
         * Get first letter of string in uppercase
         * @param {type} input
         * @returns {unresolved}
         */
        $scope.oneCapLetter = function (input) {
            return input.substring(0, 1).toLowerCase().replace(/\b[a-z]/g, function (letter) {
                return letter.toUpperCase();
            });
        }

        /*
         * To create info window for map marker
         * @param Object address
         * @param Object mapobj
         * @returns {google.maps.Marker}
         */
        $scope.createMarker = function (address, mapobj) {
            var infowindow = new google.maps.InfoWindow();
            var marker = new google.maps.Marker({
                map: mapobj,
                draggable: true,
                position: new google.maps.LatLng(address.latitude, address.longitude),
                animation: google.maps.Animation.DROP,
                icon: address.marker_icon,
                title: address.name,
                category: address.filter_by,
                //label: $scope.oneCapLetter(address.name),
            });

            google.maps.event.addListener(marker, 'click', function () {
                if (marker.getAnimation() !== null) {
                    marker.setAnimation(null);
                } else {
                    marker.setAnimation(google.maps.Animation.BOUNCE);
                }

                infowindow.close();
                infowindow.setContent(address.info_content);
                infowindow.open(mapobj, marker);
            });

            google.maps.event.addListener(infowindow, 'closeclick', function () {
                //currentMark.setMap(null); //removes the marker
                // then, remove the infowindows name from the array
                marker.setAnimation(null);
            });
            return marker;
        };
        // map functions end here

        $scope.gd = function (year) {
            return new Date(year).getFullYear();
        };

        /*
         * To show tooltip on hower on dashboard chart
         * @param {Int} x
         * @param {Int} y
         * @param {String} color
         * @param {String} contents
         * @returns {undefined}
         */
        $scope.showTooltip = function (x, y, color, contents) {
            angular.element('<div id="tooltip">' + contents + '</div>').css({
                position: 'absolute',
                display: 'none',
                top: y - 40,
                left: x - 120,
                border: '2px solid ' + color,
                padding: '3px',
                'font-size': '9px',
                'border-radius': '5px',
                'background-color': '#fff',
                'font-family': 'Verdana, Arial, Helvetica, Tahoma, sans-serif',
                opacity: 0.9
            }).appendTo("body").fadeIn(200);
        };

        /*
         * load and draw chart in dashboard
         * 
         * @param {Object} response
         * @returns {undefined}
         */
        $scope.loadFlotChart = function (response) {
            // Bind the plot hover
            $scope.UseTooltip = function () {
                angular.element("#placeholder").bind("plothover", function (event, pos, item) {
                    if (item) {
                        if (($scope.previousLabel != item.series.label) || ($scope.previousPoint != item.dataIndex)) {
                            $scope.previousPoint = item.dataIndex;
                            $scope.previousLabel = item.series.label;
                            angular.element("#tooltip").remove();

                            var x = item.datapoint[0];
                            var y = (item.datapoint[1] <= 0) ? 0 : $.formatNumber(item.datapoint[1], {format: "#,###", locale: "us"});

                            var color = item.series.color;
                            var day = new Date(x).getDate();
                            var month = $scope.monthNames[new Date(x).getMonth()];
                            var year = $scope.gd(x);

                            $scope.showTooltip(item.pageX,
                                    item.pageY,
                                    color,
                                    day + ' ' + month + ', ' + year
                                    + " : <strong>" + y +
                                    "</strong>");
                        }
                    } else {
                        angular.element("#tooltip").remove();
                        $scope.previousPoint = null;
                    }
                });
            };

            var seriesData = [];
            var obj = response.users_per_day;
            var obj2 = response.visitors_counter;

            for (var prop in obj) {
                seriesData.push({label: '&nbsp;&nbsp;' + cmodule.label_chat_requests_per_day + '&nbsp;&nbsp;', data: $.map(obj[prop], function (i, j) {
                        return [[new Date(i[0], i[1] - 1, i[2]).getTime(), i[3]]];
                    })});
            }

            for (var prop in obj2) {
                seriesData.push({label: '&nbsp;&nbsp;' + cmodule.label_visitors_per_day + '&nbsp;&nbsp;', data: $.map(obj2[prop], function (i, j) {
                        return [[new Date(i[0], i[1] - 1, i[2]).getTime(), i[3]]];
                    })});
            }

            // helper for returning the weekends in a period
            function weekendAreas(axes) {
                var markings = [];
                var d1 = new Date(axes.xaxis.min);
                // go to the first Saturday
                d1.setUTCDate(d1.getUTCDate() - ((d1.getUTCDay() + 1) % 1))
                d1.setUTCSeconds(0);
                d1.setUTCMinutes(0);
                d1.setUTCHours(0);
                var i = d1.getTime();
                do {
                    // when we don't set yaxis, the rectangle automatically
                    // extends to infinity upwards and downwards
                    markings.push({
                        xaxis: {
                            from: i,
                            to: i + 2 * 24 * 60 * 60 * 1000
                        }
                    });
                    i += 1 * 24 * 60 * 60 * 1000;
                } while (i < axes.xaxis.max);

                return markings;
            }

            $scope.fcOptions = {
                colors: ["#edc240", "#4f7cdc"],
                series: {
                    lines: {
                        show: true,
                        lineWidth: 6,
                        fill: false
                    },
                    points: {
                        show: true,
                        fillColor: "rgba(0,0,0,0.35)",
                        radius: 3,
                        lineWidth: 1.5
                    },
                    grow: {
                        active: false
                    },
                    shadowSize: 2
                },
                legend: {
                    margin: -8,
                    position: "ne",
                    noColumns: 2,
                    container: angular.element("#container-legend")
                },
                xaxis: {
                    tickColor: "#CCCCCC",
                    mode: "time",
                    timeformat: '%d %b',
                    axisLabel: "Months",
                    axisLabelUseCanvas: false,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 50,
                    //tickLength: 5                    
                },
                yaxis: {
                    tickColor: "#CCCCCC",
                    //tickLength: 5,
                    autoscaleMargin: 0.1,
                    min: 0
                },
                selection: {
                    mode: "x"
                },
                grid: {
                    tickColor: "rgba(255,255,255,0.05)",
                    markingsColor: "rgba(255,255,255,0.05)",
                    markingsLineWidth: 10,
                    markings: weekendAreas,
                    // interactive stuff
                    clickable: true,
                    hoverable: true,
                    showLines: true,
                    borderWidth: 1,
                    borderColor: "#CCCCCC",
                    backgroundColor: "#fff"
                }
            };
            $scope.previousPoint = null;
            $scope.previousLabel = null;
            var plot = angular.element.plot(angular.element("#placeholder"), seriesData, $scope.fcOptions);

            angular.element("#placeholder").bind("plotselected", function (event, ranges) {
                // do the zooming
                plot = angular.element.plot(angular.element("#placeholder"), seriesData,
                        angular.element.extend(true, {}, $scope.fcOptions, {
                            xaxis: {
                                min: ranges.xaxis.from,
                                max: ranges.xaxis.to
                            }
                        }));
            });

            $scope.claerChart = function (event) {
                event.preventDefault();

                angular.element.plot(angular.element("#placeholder"), seriesData, $scope.fcOptions);
            }
        }

        /*
         * Reload chart by filtering data
         * @returns {undefined}
         */
        $scope.reload_chart = function () {
            $scope.toggleLoder();

            $http.get(Setting.site_url + "?c=admin&m=get_chart_data&site_id=" + $scope.current_site.id + '&year=' + $scope.current_year.year + '&month=' + $scope.current_month.month).success(function (response) {
                $scope.toggleLoder();
                if (response.result == 'success') {
                    $scope.loadFlotChart(response);
                } else if (response.result == 'failed') {
                    if (response.error_type == 'login_session_expired') {
                        angular.element('#login-modal').modal('show');
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    }
                }
            });
        }

        /*
         * Will call on change year in chart filter
         * @param Event event
         * @param Object year
         * @returns {undefined}
         */
        $scope.change_year = function (event, year) {
            event.preventDefault();

            $scope.current_year = year;
            $scope.current_month = year.months[0];

            $scope.reload_chart();
        }

        /*
         * Will call on change month in chart filter
         * @param Event event
         * @param Object year
         * @returns {undefined}
         */
        $scope.change_month = function (event, month) {
            event.preventDefault();

            $scope.current_month = month;

            $scope.reload_chart();
        }

        /*
         * To fetch all access tikens
         * @returns {undefined}
         */
        $scope.get_sites = function () {
            $http.get(Setting.site_url + "?c=sites&m=get_sites").success(function (response) {
                if (response.result == 'success') {
                    $scope.sites = response.sites;
                }
            });
        }

        $scope.get_sites();

        /*
         * Reload data according selected site option
         * @param Event event
         * @param Mixed site
         * @returns {undefined}
         */
        $scope.reload_dashboard = function (event, site) {
            event.preventDefault();
            $scope.toggleLoder();
            $scope.most_rated_operators = [];

            if (site == 'all') {
                $scope.current_site = {id: 0, site_name: ''};
            } else {
                $scope.current_site = site;
            }

            $http.get(Setting.site_url + "?c=admin&m=get_dashboard_data&site_id=" + $scope.current_site.id + '&year=' + $scope.current_year.year + '&month=' + $scope.current_month.month).success(function (response) {
                $scope.toggleLoder();

                if (response.result == 'success') {
                    $scope.pageviews = response.pageviews;
                    $scope.online_requests = response.online_requests;
                    $scope.offline_requests = response.offline_requests;
                    $scope.most_rated_operators = response.most_rated_operators;
                    $scope.recent_chats = response.recent_chats;
                    $scope.operators_n_requests = response.operators_n_requests;

                    $scope.loadFlotChart(response);
                    if (Setting.settings.google_map_api_key) {
                        $scope.drawMap(response);
                    }
                } else if (response.result == 'failed') {
                    if (response.error_type == 'login_session_expired') {
                        angular.element('#login-modal').modal('show');
                        $scope.notification.showErrors = true;
                        $scope.notification.errors = response.errors;
                    }
                }
            });
        }

        /*
         * To set admin data
         */
        $http.get(site_url + "?c=admin&m=set_data");
    });
})();