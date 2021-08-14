# dex2jar
Dex2jar fork with fixes and features specifically targeted for generating stubs for [Aliucord](https://github.com/Aliucord/Aliucord).

## Downloads

**To download the latest builds, head to the [Releases](https://github.com/Aliucord/dex2jar/releases).**

Tools to work with android .dex and java .class files

1. dex-reader/writer:
   Read/write the Dalvik Executable (.dex) file. It features
   a [lightweight API similar with ASM](https://sourceforge.net/p/dex2jar/wiki/Faq/#want-to-read-dex-file-using-dex2jar)
   .
2. d2j-dex2jar:
   Convert .dex file to .class files (zipped as jar)
3. smali/baksmali:
   Disassemble dex to smali files and assemble dex from smali files. Different implementation
   to [smali/baksmali](http://code.google.com/p/smali), same syntax, but we support escape in type desc
   `"Lcom/dex2jar\t\u1234;"`
4. other tools:
   [d2j-decrypt-string](https://sourceforge.net/p/dex2jar/wiki/DecryptStrings)

## Usage

1. In the root directory run: `./gradlew shadowJar`
2. `cd build/libs`
3. Run `dex2jar.jar`

### Example usage:

```shell
java -jar dex2jar.jar --no-code -f ~/path/to/apk_to_decompile.apk
```

And the output file will be `apk_to_decompile-dex2jar.jar`.

## License

[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
