package main

import (
	"errors"
	"github.com/ncruces/zenity"
	"io"
	"io/fs"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"
)

const url = "https://github.com/mircokroon/minecraft-world-downloader/releases/latest/download/world-downloader.jar"
const jarFile = "world-downloader.jar"
const jarFileDownloading = jarFile + ".downloading"

const gameRuntimeDir = "C:\\Program Files (x86)\\Minecraft Launcher\\runtime\\"

func main() {
	if !exists(jarFile) {
		notify("Downloading " + jarFile)

		download()
		rename()
	}
	run()
}

func exists(path string) bool {
	_, err := os.Stat(path)

	return !errors.Is(err, os.ErrNotExist)
}

func download() {
	file, err := os.Create(jarFileDownloading)
	defer file.Close()
	checkError(err)

	response, err := http.Get(url)
	if response.StatusCode != 200 {
		checkError(errors.New("Got status code " + strconv.Itoa(response.StatusCode) + " while downloading <" + url + ">"))
	}
	defer response.Body.Close()
	checkError(err)

	_, err = io.Copy(file, response.Body)
	checkError(err)
}

func rename() {
	err := os.Rename(jarFileDownloading, jarFile)
	checkError(err)
}

func run() {
	success := runWithJavaPath("java") || runEach(findJavaHomeSiblings()) || runEach(findJavaExecutables(gameRuntimeDir))
	if !success {
		writeLog()
		checkError(errors.New("cannot find suitable Java version. Make sure one is correctly installed"))
	}
}

func runWithJavaPath(java string) bool {
	log("Trying to run " + java)

	// running the command through cmd and not directly lets us hide the command prompt without hiding the gui as well
	cmd := exec.Command("cmd.exe", "/C", java, "-jar", jarFile)
	cmd.SysProcAttr = &syscall.SysProcAttr{HideWindow: true}
	out, err := cmd.CombinedOutput()

	if err == nil {
		cmd.Wait()
		return true
	}

	log("\t" + err.Error())
	log("\t" + string(out))

	return false
}

// find JAVA_HOME and go up one folder to find all versions of Java (ideally)
func findJavaHomeSiblings() []string {
	env := os.Getenv("JAVA_HOME")

	if env == "" {
		return nil
	}

	return findJavaExecutables(filepath.Dir(env))
}

// find java executables in a given path
func findJavaExecutables(dir string) []string {
	options := make([]string, 0)
	filepath.WalkDir(dir, func(path string, di fs.DirEntry, err error) error {
		if strings.HasSuffix(path, "java.exe") {
			options = append(options, path)
		}
		return err
	})
	return options
}

func runEach(options []string) bool {
	for _, option := range options {
		wasSuccess := runWithJavaPath(option)

		if wasSuccess {
			return true
		}
	}
	return false
}

func checkErrorWithMessage(err error, info string) {
	if err != nil {
		zenity.Info("An error has occurred: \n\n"+err.Error()+"\n\n"+info,
			zenity.Title("Error"),
			zenity.ErrorIcon)
		os.Exit(1)
	}
}

func checkError(err error) {
	checkErrorWithMessage(err, "")
}

func notify(msg string) {
	zenity.Notify(msg, zenity.Title("Minecraft World Downloader"), zenity.InfoIcon)
}

// log file if unable to start
var logArr = make([]string, 0)

func writeLog() {
	f, _ := os.OpenFile("world-downloader-launcher.log", os.O_RDWR|os.O_CREATE|os.O_APPEND, 0666)

	for _, line := range logArr {
		f.Write([]byte(line + "\n"))
	}
}
func log(str string) {
	logArr = append(logArr, str)
}
