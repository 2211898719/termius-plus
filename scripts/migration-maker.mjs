#!/usr/bin/env zx

/**
 * 命令使用
 * 
 * ./scripts/migration-maker.mjs VERSION_NAME
 */

 const t = new Date();
 const version = [
     t.getFullYear().toString(), 
     (t.getMonth() + 1).toString().padStart(2, '0') + t.getDate().toString().padStart(2, '0'), 
     t.getHours().toString().padStart(2, '0') + t.getMinutes().toString().padStart(2, '0') + t.getSeconds().toString().padStart(2, '0')
 ].join('.');
 
 if (argv._.length !== 2) {
     console.log(chalk.red('Argument invalid.'));
     await $`exit 1`;
 }
 
 const dir = await fs.promises.realpath(__dirname + '/../server/src/main/resources/db/migration');
 cd(dir);
 
 const filename = `V${version}__${argv._[1]}.sql`;
 
 $`touch ${filename}`;
