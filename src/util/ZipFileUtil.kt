package com.github.rwsbillyang.ktorKit.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


object ZipFileUtil {
    private const val FILE_SIZE = 4 * 1024
    /**
     * 文件打包的方法
     * @param filenames 文件信息集合（文件的存放完整路径名称）
     * @param savePath 存储打包文件的路径and filename
     * @return true文件打包成功 false 文件打包失败
     */
    fun zipFilenames(filenames: List<String>, savePath: String): Boolean {
        return if (filenames.isNotEmpty()) {
            val files: MutableList<File> = ArrayList<File>(filenames.size)
            for (filename in filenames) files.add(File(filename))
            zipFiles(files, savePath)
        } else {
            error("list is empty when createFilesToZip")
        }
    }

    /**
     * 文件打包的方法
     * @param files 文件信息集合（文件的存放完整路径名称）
     * @param savePath 存储打包文件的路径and filename
     * @return true文件打包成功 false 文件打包失败
     */
    fun zipFiles(files: List<File>, savePath: String): Boolean {
        var result = false
        // 定义字节流
        val buffer = ByteArray(FILE_SIZE)
        try {
            val out = ZipOutputStream(FileOutputStream(savePath))
            if (files.isNotEmpty()) {
                for (i in files.indices) {
                    val file: File = files[i]
                    // 判断文件是否为空
                    if (file.exists()) {
                        // 创建输入流
                        val fis = FileInputStream(file)
                        // 获取文件名
                        val name: String = file.name
                        // 创建zip对象
                        val zipEntry = ZipEntry(name)
                        out.putNextEntry(zipEntry)
                        var len: Int
                        // 读入需要下载的文件的内容，打包到zip文件
                        while (fis.read(buffer).also { len = it } > 0) {
                            out.write(buffer, 0, len)
                        }
                        out.closeEntry()
                        fis.close()
                    }
                }
            }
            out.close()
            result = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}