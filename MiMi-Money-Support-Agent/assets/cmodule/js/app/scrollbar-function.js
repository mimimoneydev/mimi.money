(function ($) {
    function control_filter_height() {
        var body_height = $('body').height();
        $('.filter-inner form').css('height', body_height - 115);
    }

    function control_agent_sidebar_height() {
        var body_height = $('body').height();
        var site_header_height = $('.site-header').outerHeight();
        var user_info_height = $('.page-sidebar .user-info-block').outerHeight();
        var user_tab_height = $('.user-panel .nav-tabs').outerHeight();

        $('.chat-sidebar').css('height', body_height - (site_header_height + user_info_height + user_tab_height + 20));
    }

    $(window).load(function () {
        control_filter_height();
        control_agent_sidebar_height();

        $(".sidebar-recent-chats, .mCustomScrollbar, .filter-inner form").mCustomScrollbar();
    });
    
    $(window).resize(function () {
        control_filter_height();
        control_agent_sidebar_height();

        $(".sidebar-recent-chats, .mCustomScrollbar, .filter-inner form").mCustomScrollbar("update");
    });

})(jQuery);