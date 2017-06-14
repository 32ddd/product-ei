/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.ei.mb.test.utils;

import java.io.File;

/**
 * Utilities for files and directory related operations
 */
public class FileManipulator {

    /**
     * Recursively delete directories exist in given path including sub-directories/files.
     *
     * @param directory directory path
     * @return
     */
    public static boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            String[] childFilePathList = directory.list();
            if (childFilePathList != null) {
                for (String child : childFilePathList) {
                    boolean success = deleteDirectory(new File(directory, child));

                    // If delete directory failed returns false.
                    if (!success) {
                        return false;
                    }
                }
            }
        }

        return directory.delete();
    }



}
