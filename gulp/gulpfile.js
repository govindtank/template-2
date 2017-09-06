var gulp = require('gulp');
var zip = require('gulp-zip');
var eslint = require('gulp-eslint');
var uglify = require('gulp-uglify');
var browserSync = require('browser-sync');
var rename = require("gulp-rename");
var fileInclude = require('gulp-file-include');
var sass = require('gulp-sass');
var autoPreFixer = require('gulp-autoprefixer');
var del = require('del');
var Module = require('./module.js');
var imageMin = require('gulp-imagemin');
var proxy = require('http-proxy-middleware');

//==========================================================================================
// build
//==========================================================================================

//images
function buildImages(module) {
    return gulp.src(module.modulePathImg + '/*').pipe(imageMin()).pipe(gulp.dest(module.buildModulePathImg));
}

//sass
function buildScss(module) {
    return gulp.src(module.modulePathScss + '/*.scss')
        .pipe(sass({precision: 10, outputStyle: 'compact', errLogToConsole: true}).on('error', sass.logError))
        .pipe(autoPreFixer({browsers: ['> 1%', 'Last 10 versions', 'IE 8'], cascade: true, remove: true}))
        .pipe(rename({suffix: '', extname: '.css'}))
        .pipe(gulp.dest(module.buildModulePathScss))
        .pipe(browserSync.reload({stream: true}));
}

//scripts
function buildJs(module) {
    return gulp.src(module.modulePathJs + '/*.js')
        .pipe(eslint({configFle: "./.eslintrc.js"}))
        .pipe(eslint.format())
        .pipe(eslint.failOnError())
        .pipe(uglify())
        .pipe(rename({suffix: ''}))
        .pipe(gulp.dest(module.buildModulePathJs));
}

//html
function buildHtml(module) {
    return gulp.src(module.moduleTemplateFilePath)
        .pipe(fileInclude({prefix: '@@', basepath: '@file', context: {moduleName: module.moduleName, htmlName: 'index'}}))
        .pipe(rename({basename: 'index', extname: '.html'}))
        .pipe(gulp.dest(module.buildModulePathHtml));
}

//watch
function watch(module) {
    gulp.watch([module.modulePathJs + '/*.js'], gulp.series(cleanJsTask, buildJsTask, reloadTask, zipJsTask));
    gulp.watch([module.modulePathScss + '/*.scss'], gulp.series(cleanScssTask, buildScssTask, reloadTask, zipScssTask));
    gulp.watch([module.modulePathHtml + '/*.html', module.moduleTemplateFilePath], gulp.series(cleanHtmlTask, buildHtmlTask, reloadTask, zipHtmlTask));
    gulp.watch([module.modulePathImg + '/*'], gulp.series(cleanImagesTask, buildImagesTask, reloadTask, zipImagesTask));
}

function zipJs(module) {
    return gulp.src(module.buildModulePathJs + '/*')
        .pipe(zip(module.moduleName + '-js.zip'))
        .pipe(gulp.dest(module.buildModulePath))
}

function zipImages(module) {
    return gulp.src(module.buildModulePathImg + '/*')
        .pipe(zip(module.moduleName + '-images.zip'))
        .pipe(gulp.dest(module.buildModulePath))
}

function zipScss(module) {
    return gulp.src(module.buildModulePathScss + '/*')
        .pipe(zip(module.moduleName + '-css.zip'))
        .pipe(gulp.dest(module.buildModulePath))
}

function zipHtml(module) {
    return gulp.src(module.buildModulePathHtml + '/index.html')
        .pipe(zip(module.moduleName + '-html.zip'))
        .pipe(gulp.dest(module.buildModulePath))
}

//remove images build
function cleanImages(module) {
    return del([module.buildModulePathImg, module.moduleName + '-images.zip']);
}

function cleanHtml(module) {
    return del([module.buildModulePathHtml, module.moduleName + '-html.zip']);
}

function cleanScss(module) {
    return del([module.buildModulePathScss, module.moduleName + '-css.zip']);
}

function cleanJs(module) {
    return del([module.buildModulePathJs, module.moduleName + '-js.zip']);
}

//==========================================================================================
// tasks
//==========================================================================================
function zipAll(done) {
    zipJs(module);
    zipImages(module);
    zipScss(module);
    zipHtml(module);
    return done();
}

function zipJsTask() {
    return zipJs(module);
}

function zipImagesTask() {
    return zipImages(module);
}

function zipScssTask() {
    return zipScss(module);
}

function zipHtmlTask() {
    return zipHtml(module);
}

function buildImagesTask() {
    return buildImages(module);
}

function buildScssTask() {
    return buildScss(module);
}

function buildJsTask() {
    return buildJs(module);
}

function buildHtmlTask() {
    return buildHtml(module);
}

function watchTask() {
    return watch(module);
}

function cleanImagesTask() {
    return cleanImages(module);
}

function cleanJsTask() {
    return cleanJs(module);
}

function cleanScssTask() {
    return cleanScss(module);
}

function cleanHtmlTask() {
    return cleanHtml(module);
}

function cleanTask() {
    return del(['build']);
}

function reloadTask(done) {
    browserSync.reload();
    done();
}

function syncTask() {
    return browserSync({
        server: {
            baseDir: module.buildModulePathHtml
        },
        port: 9999
    });
}

//==========================================================================================
// default
//==========================================================================================

var module = new Module('20170909');

gulp.task('default',
    gulp.series(
        cleanTask,
        gulp.parallel(buildImagesTask, buildScssTask, buildJsTask, buildHtmlTask),
        gulp.parallel(syncTask, watchTask, zipAll)
    )
);