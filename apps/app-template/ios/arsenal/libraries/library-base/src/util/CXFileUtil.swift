import Foundation

class CXFileUtil {
    static let TAG = "[file]"

    static func deleteFile(_ path: String?) {
        do {
            try FileManager.default.removeItem(atPath: path!)
            CXLogUtil.d(TAG + ":deleteFile", "[文件删除成功] path:", path!, " , fileExists:", CXFileUtil.fileExists(path))
        } catch {
            CXLogUtil.d(TAG + ":deleteFile", "[文件删除失败] path:", path!, " , fileExists:", CXFileUtil.fileExists(path), error)
        }
    }

    static func deleteDirectory(_ path: String?) {
        do {
            try FileManager.default.removeItem(atPath: path!)
            CXLogUtil.d(TAG + ":deleteDirectory", "[目录删除成功] path:", path!, " , fileExists:", CXFileUtil.fileExists(path))
        } catch {
            CXLogUtil.d(TAG + ":deleteDirectory", "[目录删除失败] path:", path!, " , fileExists:", CXFileUtil.fileExists(path), error)
        }
        CXFileUtil.makeDirs(path)
    }

    static func makeDirs(_ path: String?) {
        do {
            try FileManager.default.createDirectory(atPath: path!, withIntermediateDirectories: true, attributes: nil)
            CXLogUtil.d(TAG + ":makeDirs", "[路径创建成功] path:", path!, " , fileExists:", CXFileUtil.fileExists(path))
        } catch {
            CXLogUtil.d(TAG + ":makeDirs", "[路径创建失败] path:", path!, " , fileExists:", CXFileUtil.fileExists(path), error)
        }
    }

    static func copy(_ fromFilePath: String, _ toFilePath: String) throws {
        CXLogUtil.d(TAG + ":copy-start", "from:", fromFilePath, " , fileExists:", FileManager.default.fileExists(atPath: fromFilePath))
        try FileManager.default.copyItem(atPath: fromFilePath, toPath: toFilePath)
        CXLogUtil.d(TAG + ":copy-end", "to:", toFilePath, " , fileExists:", FileManager.default.fileExists(atPath: toFilePath))
    }

    static func printDirs(_ dirPath: String) {
        let files = FileManager.default.subpaths(atPath: dirPath)

        CXLogUtil.d(TAG + ":printDirs", "============================================")
        CXLogUtil.d(TAG + ":printDirs", "dirPath:", dirPath)
        CXLogUtil.d(TAG + ":printDirs", "============================================")
        if (files != nil) {
            for file in files! {
                CXLogUtil.d(TAG + ":printDirs", file)
            }
        }
        CXLogUtil.d(TAG + ":printDirs", "============================================")
    }

    static func fileExists(_ atPath: String?) -> Bool {
        return atPath != nil && !atPath!.isEmpty && FileManager.default.fileExists(atPath: atPath!)
    }
}