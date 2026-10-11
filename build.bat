@echo off
rem Project build script for Windows (mirror of build.sh).
rem   build.bat              compile everything into out\
rem   build.bat run [...]    compile and launch the game
rem   build.bat test         compile into out-test\ and run the headless suite
rem                          (each test isolated; separate tree from out\ so the
rem                          suite and the game never clobber each other)
rem   build.bat strip        regenerate stripped\ and verify it still compiles
rem   build.bat jar [plat]   fat release jar into dist\ (default windows-x64)
setlocal EnableDelayedExpansion
cd /D "%~dp0"
set ROOT=%CD%

rem --- JDK discovery (no machine-specific paths): JAVA_HOME if 17+, else
rem --- javac on PATH if 17+, else a Temurin-style install. 17.x preferred.
set JAVAC_BIN=
set JAVA_BIN=
set JAR_BIN=
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
  call :check_javac "%JAVA_HOME%\bin\javac.exe"
  if !JVMAJOR! GEQ 17 (
    set "JAVAC_BIN=%JAVA_HOME%\bin\javac.exe"
    set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
    set "JAR_BIN=%JAVA_HOME%\bin\jar.exe"
  )
)
if not defined JAVAC_BIN (
  where javac.exe >nul 2>nul
  if not errorlevel 1 (
    for /f "tokens=2" %%v in ('javac -version 2^>^&1') do set JV=%%v
    for /f "tokens=1 delims=." %%m in ("!JV!") do set JVMAJOR=%%m
    if !JVMAJOR! GEQ 17 (
      set JAVAC_BIN=javac
      set JAVA_BIN=java
      set JAR_BIN=jar
    )
  )
)
if not defined JAVAC_BIN (
  for %%d in ("%ProgramFiles%\Eclipse Adoptium" "%ProgramFiles%\Java" "%ProgramFiles%\Zulu") do (
    if exist "%%~d" (
      for /D %%j in ("%%~d\jdk-17*" "%%~d\zulu-17*") do (
        if exist "%%~j\bin\javac.exe" (
          call :check_javac "%%~j\bin\javac.exe"
          if !JVMAJOR! GEQ 17 (
            set "JAVAC_BIN=%%~j\bin\javac.exe"
            set "JAVA_BIN=%%~j\bin\java.exe"
            set "JAR_BIN=%%~j\bin\jar.exe"
            goto jdk_done
          )
        )
      )
    )
  )
)
:jdk_done
if not defined JAVAC_BIN (
  echo need a JDK 17+: set JAVA_HOME or put javac 17+ on PATH 1>&2
  exit /b 1
)
set LIBDIR=%ROOT%\lib
set LIB=%LIBDIR%\lwjgl.jar;%LIBDIR%\lwjgl_util.jar;%LIBDIR%\jinput.jar
set JAVA_FLAGS=-Xms256m
if "%~1"=="" goto build
if /I "%~1"=="build" goto build
if /I "%~1"=="run" goto run
if /I "%~1"=="test" goto test
if /I "%~1"=="strip" goto strip
if /I "%~1"=="jar" goto jar
echo usage: build.bat [build^|run^|test^|strip^|jar] 1>&2
exit /b 1

:check_javac
set JV=
set JVMAJOR=0
for /f "tokens=2" %%v in ('"%~1" -version 2^>^&1') do set JV=%%v
for /f "tokens=1 delims=." %%m in ("!JV!") do set JVMAJOR=%%m
exit /b 0

:emit_java
rem %1 = root dir to walk. Appends slash-normalized quoted paths to %LISTFILE%.
rem javac @files treat backslash as an escape, so Windows paths must use
rem forward slashes inside the file (fine everywhere for javac).
for /R %1 %%f in (*.java) do @(
  set "p=%%f"
  set "p=!p:\=/!"
  echo "!p!" >> "%LISTFILE%"
)
exit /b 0

:compile_one
rem %1 = target tree (out or out-test). Wipes, copies res, compiles.
if exist "%~1" rmdir /S /Q "%~1"
mkdir "%~1"
xcopy res "%~1" /E /I /Y >nul
rem Alt-discovery index, like build.sh: tile listing so the first stitch
rem does not probe getResourceAsStream x99 per tile (absent index falls
rem back to probing - slow, correct).
if exist res\textures (
  if exist res\textures\index.txt del res\textures\index.txt
  (for /R res\textures %%t in (*.png) do @(
    set "p=%%t"
    set "p=!p:*res\textures\=!"
    set "p=!p:\=/!"
    echo !p!
  )) > "%TEMP%\strata-index.txt"
  sort "%TEMP%\strata-index.txt" /O res\textures\index.txt
  del "%TEMP%\strata-index.txt"
)
if exist test (
  del "%TEMP%\strata-src.txt" 2>nul
  set LISTFILE=%TEMP%\strata-src.txt
  call :emit_java src
  call :emit_java test
) else (
  del "%TEMP%\strata-src.txt" 2>nul
  set LISTFILE=%TEMP%\strata-src.txt
  call :emit_java src
)
"%JAVAC_BIN%" -encoding UTF-8 -cp "%LIB%" -d "%~1" @"%TEMP%\strata-src.txt"
if errorlevel 1 exit /b 1
for %%c in (com\strata\world\Level.class com\strata\Boot.class com\strata\client\GameClient.class com\strata\world\gen\VegTest.class com\strata\DepTest.class) do (
  if not exist "%~1\%%c" (
    echo compile incomplete ^(missing %~1\%%c^) - fix the javac errors above 1>&2
    exit /b 1
  )
)
exit /b 0

:build
call :compile_one out
if errorlevel 1 exit /b 1
echo BUILD_OK
exit /b 0

:run
call :compile_one out
if errorlevel 1 exit /b 1
echo BUILD_OK
set GAMEARGS=
:getargs
shift
if "%~1"=="" goto runexec
set GAMEARGS=%GAMEARGS% %1
goto getargs
:runexec
"%JAVA_BIN%" %JAVA_FLAGS% -cp "out;res;%LIB%" com.strata.Boot%GAMEARGS%
exit /b 0

:test
call :compile_one out-test
if errorlevel 1 exit /b 1
echo BUILD_OK
if exist "%TEMP%\rbtest" rmdir /S /Q "%TEMP%\rbtest"
set FOUND=0
set FAILED=0
for /R out-test %%f in (*Test.class) do (
  set "c=%%f"
  set "c=!c:*out-test\=!"
  echo !c! | find "$" >nul
  if not errorlevel 1 (
    rem inner-class file, not a runnable test
  ) else (
    set FOUND=1
    set "c=!c:.class=!"
    set "c=!c:\=.!"
    set "d=!c:.=_!"
    mkdir "%TEMP%\rbtest\!d!" 2>nul
    echo === !c! ===
    pushd "%TEMP%\rbtest\!d!"
    "%JAVA_BIN%" %JAVA_FLAGS% -Djava.awt.headless=true -cp "%ROOT%\out-test;%ROOT%\res;%LIB%" !c!
    if errorlevel 1 set FAILED=1
    popd
  )
)
if exist "%TEMP%\rbtest" rmdir /S /Q "%TEMP%\rbtest"
if "%FAILED%"=="1" exit /b 1
if "%FOUND%"=="0" echo (no tests in this tree)
exit /b 0

:strip
if not exist tools\strip.py (
  echo strip unavailable in this tree ^(dev-only tool^) 1>&2
  exit /b 1
)
set PYBIN=
where py >nul 2>nul
if not errorlevel 1 set PYBIN=py -3
if not defined PYBIN (
  where python >nul 2>nul
  if not errorlevel 1 set PYBIN=python
)
if not defined PYBIN (
  echo strip needs Python 3 ^(py launcher or python on PATH^) 1>&2
  exit /b 1
)
if exist stripped rmdir /S /Q stripped
%PYBIN% tools\strip.py
if errorlevel 1 exit /b 1
if exist "%TEMP%\stripcheck" rmdir /S /Q "%TEMP%\stripcheck"
mkdir "%TEMP%\stripcheck"
del "%TEMP%\strip-files.txt" 2>nul
set LISTFILE=%TEMP%\strip-files.txt
call :emit_java stripped\src
if exist stripped\test call :emit_java stripped\test
"%JAVAC_BIN%" -encoding UTF-8 -cp "%LIB%" -d "%TEMP%\stripcheck" @"%TEMP%\strip-files.txt"
if errorlevel 1 exit /b 1
echo STRIP_OK
exit /b 0

:jar
set PLAT=windows-x64
if not "%~2"=="" set PLAT=%~2
if /I not "%PLAT%"=="mac-arm64" if /I not "%PLAT%"=="mac-x64" if /I not "%PLAT%"=="windows-x64" if /I not "%PLAT%"=="linux-x64" (
  echo unknown platform '%PLAT%' ^(mac-arm64^|mac-x64^|windows-x64^|linux-x64^) 1>&2
  exit /b 1
)
if not exist "natives\%PLAT%" (
  echo no vendored natives for %PLAT% 1>&2
  exit /b 1
)
call :compile_one out
if errorlevel 1 exit /b 1
for /f tokens^=2^ delims^=^" %%v in ('findstr /C:"VERSION = " src\com\strata\core\Config.java') do set VER=%%v
if exist dist\work rmdir /S /Q dist\work
del "dist\Strata-%VER%-%PLAT%.jar" 2>nul
mkdir dist\work
xcopy out dist\work\ /E /I /Y >nul
pushd dist\work
for %%j in (..\..\lib\*.jar) do "%JAR_BIN%" xf "%%~fj"
popd
rmdir /S /Q dist\work\META-INF
mkdir dist\work\native
xcopy "natives\%PLAT%\*" dist\work\native\ /Y >nul
if exist viewer-%PLAT%.exe (
  mkdir dist\work\terminal >nul 2>nul
  copy viewer-%PLAT%.exe dist\work\terminal\viewer.exe >nul
  goto jar_viewer_done
)
if exist viewer-%PLAT% (
  mkdir dist\work\terminal >nul 2>nul
  copy viewer-%PLAT% dist\work\terminal\viewer >nul
  goto jar_viewer_done
)
if /I "%PLAT%"=="mac-arm64" if exist viewer (
  mkdir dist\work\terminal >nul 2>nul
  copy viewer dist\work\terminal\viewer >nul
  goto jar_viewer_done
)
echo note: no %PLAT% viewer binary ^(viewer-%PLAT%^), jar ships without the terminal viewer 1>&2
:jar_viewer_done
del /S dist\work\.DS_Store 2>nul
mkdir dist\work\META-INF
> dist\work\META-INF\MANIFEST.MF echo Manifest-Version: 1.0
>>dist\work\META-INF\MANIFEST.MF echo Main-Class: com.strata.Boot
>>dist\work\META-INF\MANIFEST.MF echo.
pushd dist\work
"%JAR_BIN%" cfm "..\Strata-%VER%-%PLAT%.jar" META-INF\MANIFEST.MF .
popd
rmdir /S /Q dist\work
dir "dist\Strata-%VER%-%PLAT%.jar"
exit /b 0
