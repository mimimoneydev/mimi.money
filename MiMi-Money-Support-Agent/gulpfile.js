var gulp = require('gulp');
var concat = require('gulp-concat');
var uglifycss = require('gulp-uglifycss');
var uglify = require('gulp-uglify');

gulp.task('compress-widget-css', function (cb) {
    gulp.src('assets/cmodule-chat/css/chatbox-widget.css')
            .pipe(uglifycss({
                "maxLineLen": 0,
                "uglyComments": false
            }))
            .pipe(concat('chatbox-widget.min.css'))
            .pipe(gulp.dest('./assets/cmodule-chat/css/'));
});

gulp.task('compress-desk-css', function () {
    gulp.src(
            [
                'assets/cmodule-chat/css/cmodule-chat.css',
                'assets/cmodule-chat/css/font-awesome.min.css',
                'assets/angular-rangeslider-directive-master/angular-range-slider.css',
                'assets/scrollbar-plugin/css/jquery.mCustomScrollbar.css',
                'assets/angular-smilies/dist/angular-smilies-embed.min.css'
            ]
            )
            .pipe(uglifycss({
                "maxLineLen": 0,
                "uglyComments": false
            }))
            .pipe(concat('chatbox-desktop.min.css'))
            .pipe(gulp.dest('./assets/cmodule-chat/css/'));
});

gulp.task('compress-mobile-css', function () {
    gulp.src(
            [
                'assets/cmodule-chat/css/cmodule-chat.css',
                'assets/cmodule-chat/css/font-awesome.min.css',
                'assets/angular-rangeslider-directive-master/angular-range-slider.css',
                'assets/scrollbar-plugin/css/jquery.mCustomScrollbar.css',
                'assets/angular-smilies/dist/angular-smilies-embed.min.css',
                'assets/cmodule-chat/css/cmodule-responsive-chat.css'
            ]
            )
            .pipe(uglifycss({
                "maxLineLen": 0,
                "uglyComments": false
            }))
            .pipe(concat('chatbox-mobile.min.css'))
            .pipe(gulp.dest('./assets/cmodule-chat/css/'));
});

gulp.task('compress-desk-js', function (cb) {
    gulp.src(
            [
                'assets/cmodule-chat/js/angularjs/jquery-1.8.0.min.js',
                'assets/scrollbar-plugin/js/jquery.mCustomScrollbar.concat.min.js',
                'assets/cmodule-chat/js/angularjs/angular.min.js',
                'assets/cmodule-chat/js/angularjs/angular-sanitize.min.js',
                'assets/cmodule-chat/js/angularjs/angular-animate.min.js',
                'assets/angular-bootstrap/ui-bootstrap-tpls.min.js',
                'assets/angular-rangeslider-directive-master/angular-range-slider.min.js',
                'assets/angular-smilies/dist/angular-smilies.js',
                'assets/angular-base64-upload/dist/angular-base64-upload.min.js',
                'assets/cmodule-chat/js/app-desktop.js',
                'assets/cmodule-chat/js/iframeResizer.contentWindow.min.js'
            ]
            )
            .pipe(uglify())
            .pipe(concat('chatbox-desktop.min.js'))
            .pipe(gulp.dest('./assets/cmodule-chat/js/'));
});

gulp.task('compress-mobile-js', function (cb) {
    gulp.src(
            [
                'assets/cmodule-chat/js/angularjs/jquery-1.8.0.min.js',
                'assets/scrollbar-plugin/js/jquery.mCustomScrollbar.concat.min.js',
                'assets/cmodule-chat/js/angularjs/angular.min.js',
                'assets/cmodule-chat/js/angularjs/angular-sanitize.min.js',
                'assets/angular-bootstrap/ui-bootstrap-tpls.min.js',
                'assets/angular-rangeslider-directive-master/angular-range-slider.min.js',
                'assets/angular-smilies/dist/angular-smilies.js',
                'assets/angular-base64-upload/dist/angular-base64-upload.min.js',
                'assets/cmodule-chat/js/app-mobile.js'
            ]
            )
            .pipe(uglify())
            .pipe(concat('chatbox-mobile.min.js'))
            .pipe(gulp.dest('./assets/cmodule-chat/js/'));
});

gulp.task('compress-chatbox-init', function (cb) {
    gulp.src('assets/cmodule-chat/js/chatbox-init.js')
            .pipe(uglify())
            .pipe(concat('chatbox-init.min.js'))
            .pipe(gulp.dest('./assets/cmodule-chat/js/'));
});

gulp.task('compress-chatbox-init2', function (cb) {
    gulp.src('assets/cmodule-chat/js/chatbox-init2.js')
            .pipe(uglify())
            .pipe(concat('chatbox-init2.min.js'))
            .pipe(gulp.dest('./assets/cmodule-chat/js/'));
});

gulp.task('default', [
    // css tasks
    'compress-widget-css',
    'compress-desk-css',
    'compress-mobile-css',
    // js tasks
    'compress-desk-js',
    'compress-mobile-js',
    'compress-chatbox-init',
    'compress-chatbox-init2'
]);

gulp.task('css', [
    'compress-widget-css',
    'compress-desk-css',
    'compress-mobile-css'
]);

gulp.task('js', [
    'compress-desk-js',
    'compress-mobile-js',
    'compress-chatbox-init',
    'compress-chatbox-init2'
]);