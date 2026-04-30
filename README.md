## cracked rsa or something
detailed instructions on how to run because critical thinking is a skill many lack

1. precompiled loader
   - download RsLoader-precomp.jar and put this in your mod folder
   - download https://nodejs.org
   - download rsaloader-backend directory
   - open terminal, do `node src/server.js`
   - launch game

2. compile the loader yourself (only supported on linux cry me a river)
   - clone the repo
   - use this compilation script
     ```       
      #!/usr/bin/env bash
      
      FABRIC_JAR=$(find ~/.gradle -name "fabric-loader-0.18.4.jar" | grep -v sources | head -1)
      $(find ~/.gradle -name "sponge-mixin-0.17.0*.jar" | grep -v sources | head -1)
      ASM_JAR=$(find ~/.gradle/caches -name "asm-9.9.jar" | head -1)
      ASM_TREE=$(find ~/.gradle/caches -name "asm-tree-9*.jar" | head -1)
      ASM_COMMONS=$(find ~/.gradle/caches -name "asm-commons-9*.jar" | head -1)
      
      CP="$FABRIC_JAR:$MIXIN_JAR:$ASM_JAR"
      [ -n "$ASM_TREE" ] && CP="$CP:$ASM_TREE"
      [ -n "$ASM_COMMONS" ] && CP="$CP:$ASM_COMMONS"
      
      rm -rf /tmp/rsl-build && mkdir -p /tmp/rsl-build/classes
      
      find kaleb/RS-Loader/src -name "*.java" | xargs javac --release 21 -proc:none \
          -cp "$CP" -d /tmp/rsl-build/classes 2>&1
      echo "Compile: $?"
      
      rsync -a --exclude='*.class' kaleb/RS-Loader/resources/ /tmp/rsl-build/classes/
      printf 'Manifest-Version: 1.0\r\n\r\n' > /tmp/rsl-build/MANIFEST.MF
      
      jar --create --file=kaleb/RS-Loader/RsLoader.jar \
          --manifest=/tmp/rsl-build/MANIFEST.MF \
          -C /tmp/rsl-build/classes .
      echo "comp: $?"
      ls -lh kaleb/RS-Loader/RsLoader.jar
      ```
     - Put RsLoader.jar in your mod folder
     - download https://nodejs.org
     - cd rsaloader-backend
     - open terminal, do `node src/server.js`
     - launch game
