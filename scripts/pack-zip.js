
const fs = require('fs');
const archiver = require('archiver');
const path = 'dist/' + process.env.npm_package_name + '-' + process.env.npm_package_version + '.zip';
const output = fs.createWriteStream(path);
const archive = archiver('zip', {zlib: { level: 9 }});
archive.on('err', function(err){ throw err; });
archive.on('finish', function(){ console.log('finish: ' + path) });
output.on('close', function() { console.log('wrote ' + archive.pointer() + ' bytes') });

archive.pipe(output);
archive.glob('public/**/*', {ignore: ['public/cljs/manifest.edn']});
archive.finalize();

