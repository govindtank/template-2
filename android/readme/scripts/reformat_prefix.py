#!/usr/bin/python
# -*- coding: UTF-8 -*-
import collections
import os
import shutil

# ======================================================================================================================
# 批量处理文件名称前缀，代码也会相应修改
# 只需要修改这里...                            ---- by michael.mao at 20170703
# ======================================================================================================================
OLD_FILE_PATH = "/Users/maokangren/workspace/fruit-shop/fruit-shop-android"
NEW_FILE_PATH = OLD_FILE_PATH + "-new"
OLD_CODE_FILE_PREFIX = "CX"
NEW_CODE_FILE_PREFIX = "FS"
OLD_RES_FILE_PREFIX = "fs"
NEW_RES_FILE_PREFIX = "fs"

REPLACE_PACkAGE_NAMES = collections.OrderedDict((
    ("com.xixi.library.android.util", "com.xixi.library.android.util"),
    ("com.xixi.library.android.base", "com.xixi.library.android.base"),
    ("com.xixi.library.android.widget", "com.xixi.library.android.widget"),
))

# ======================================================================================================================

excludeFiles = ["build", ".DS_Store", ".gradle", ".idea"]  # 过滤掉的的文件,文件名完全等于
excludeSuffixFiles = ["iml"]  # 过滤掉的的文件,文件名后缀等于
nameList = []
suffix_media = ["jpg", "png", "svg", "mp3", "mp4", "avi"]
suffix_source = ["java", "kt", "xml", "pro", "text", "txt", "md", "c", "cpp", "gitignore", "gradle", "py",
                 "html", "js", "css"]

replace_map = {
    OLD_CODE_FILE_PREFIX + "AppTheme": NEW_CODE_FILE_PREFIX + "AppTheme",
    OLD_CODE_FILE_PREFIX + "Widget": NEW_CODE_FILE_PREFIX + "Widget",
    OLD_CODE_FILE_PREFIX + "Dialog": NEW_CODE_FILE_PREFIX + "Dialog",
    OLD_RES_FILE_PREFIX + "_": NEW_RES_FILE_PREFIX + "_",
    "_" + OLD_RES_FILE_PREFIX: "_" + NEW_RES_FILE_PREFIX,
    ":" + OLD_RES_FILE_PREFIX: ":" + NEW_RES_FILE_PREFIX,
    "\"" + OLD_RES_FILE_PREFIX: "\"" + NEW_RES_FILE_PREFIX,
}


def init_names(filepath):
    dir_list = os.listdir(filepath)
    for file_path in dir_list:
        tmp_path = os.path.join(filepath, file_path)
        if os.path.isdir(tmp_path):
            init_names(tmp_path)
        else:
            if "." in file_path:
                nameList.append(file_path.split(".")[0])
            else:
                nameList.append(file_path)


def reformat_prefix(filepath):
    dir_list = os.listdir(filepath)
    for file_path in dir_list:

        if file_path in excludeFiles:
            print "exclude:", file_path
            continue

        tmp_path = os.path.join(filepath, file_path)
        if os.path.isdir(tmp_path):
            reformat_prefix(tmp_path)
        else:
            new_path = reformat_path(tmp_path)
            if "." in file_path:
                suffix = tmp_path.split(".")[1]
                if suffix in excludeSuffixFiles:
                    print "exclude:", file_path
                    continue
                if suffix in suffix_source:
                    fopen = open(tmp_path, "r")
                    modified = False
                    new_lines = []
                    for each_line in fopen:
                        for key in nameList:
                            if key in each_line:
                                each_line = each_line.replace(key,
                                                              key
                                                              .replace(" " + OLD_CODE_FILE_PREFIX,
                                                                       " " + NEW_CODE_FILE_PREFIX)
                                                              .replace(OLD_RES_FILE_PREFIX + "_",
                                                                       NEW_RES_FILE_PREFIX + "_")
                                                              )
                                modified = True
                        for k, v in replace_map.items():
                            each_line = each_line.replace(k, v)
                        for k, v in REPLACE_PACkAGE_NAMES.items():
                            each_line = each_line.replace(k, v)
                        new_lines.append(each_line)
                    fopen.close()
                    if modified:
                        write_file(suffix, "source", tmp_path, new_path, new_lines)
                    else:
                        rename_file_to(suffix, "source", tmp_path, new_path)
                else:
                    if suffix in suffix_media:
                        rename_file_to(suffix, "media", tmp_path, new_path)
                    else:
                        rename_file_to(suffix, "media", tmp_path, new_path)


def reformat_path(newpath):
    return newpath \
        .replace(OLD_FILE_PATH, NEW_FILE_PATH) \
        .replace(OLD_CODE_FILE_PREFIX, NEW_CODE_FILE_PREFIX) \
        .replace(OLD_RES_FILE_PREFIX + "_", NEW_RES_FILE_PREFIX + "_")


def rename_file_to(suffix, _type, oldpath, newpath):
    for k, v in REPLACE_PACkAGE_NAMES.items():
        newpath = newpath.replace(k.replace(".", "/"), v.replace(".", "/"))
    make_dirs(newpath)
    # os.rename(oldpath, newpath)
    shutil.copyfile(oldpath, newpath)
    print "[rename to](suffix:" + suffix + ")(type:" + _type + "): ", oldpath, " -> ", newpath


def make_dirs(newpath):
    try:
        dir_list = newpath.split("/")
        if len(dir_list) > 0:
            dirpath = newpath.replace(dir_list[len(dir_list) - 1], "")
            if not os.path.exists(dirpath):
                os.makedirs(dirpath)
    except IOError:
        pass


def write_file(suffix, _type, oldpath, newpath, line_list):
    for k, v in REPLACE_PACkAGE_NAMES.items():
        newpath = newpath.replace(k.replace(".", "/"), v.replace(".", "/"))
    make_dirs(newpath)
    fopen = open(newpath, 'w+')
    fopen.writelines(line_list)
    fopen.close()
    print "[write  to](suffix:" + suffix + ")(type:" + _type + "): ", oldpath, " -> ", newpath


if __name__ == '__main__':
    print "reformat prefix start --->"
    if not os.path.exists(NEW_FILE_PATH):
        os.makedirs(NEW_FILE_PATH)
        print "生成新项目文件夹:", NEW_FILE_PATH
    else:
        print "开始删除旧产物:", NEW_FILE_PATH
        shutil.rmtree(NEW_FILE_PATH)
        print "删除旧产物成功"
        os.makedirs(NEW_FILE_PATH)
        print "生成新项目文件夹:", NEW_FILE_PATH

    init_names(OLD_FILE_PATH)
    reformat_prefix(OLD_FILE_PATH)
    print "reformat prefix success !!!"
