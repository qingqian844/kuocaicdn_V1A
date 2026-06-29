var gulp = require('gulp'),
	rename = require('gulp-rename'),
	uglify = require('gulp-uglify'),
	webpack = require('webpack'),
	webpackStream = require('webpack-stream');

gulp.task('js-build', function () {
	return gulp.src('./src/js/hs-step-form.js')
		.pipe(webpackStream({
			mode: 'development',
			output: {
				library: 'HSStepForm',
				libraryTarget: 'umd',
				libraryExport: 'default',
				filename: 'hs-step-form.js',
			},
			module: {
				rules: [
					{
						test: /\.(js)$/,
						exclude: /(node_modules)/,
						loader: 'babel-loader',
						query: {
							presets: ["@babel/preset-env"]
						}
					}
				]
			},
			externals: {
				jquery: 'jQuery'
			}
		}))
		.pipe(gulp.dest('./dist/'))
		.pipe(uglify())
		.pipe(rename({
			suffix: '.min'
		}))
		.pipe(gulp.dest('./dist/'))
});

gulp.task('main-watch', function () {
	gulp.watch('./src/js/hs-step-form.js', gulp.series('js-build'));
});

// Default Task
gulp.task('default', gulp.series('main-watch'));
