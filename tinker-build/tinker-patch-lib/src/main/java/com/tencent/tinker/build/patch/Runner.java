/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.tinker.build.patch;

import com.tencent.tinker.build.builder.PatchBuilder;
import com.tencent.tinker.build.decoder.ApkDecoder;
import com.tencent.tinker.build.info.PatchInfo;
import com.tencent.tinker.build.util.Logger;
import com.tencent.tinker.build.util.TinkerPatchException;

import java.io.IOException;

/**
 * Created by zhangshaowen on 2/26/16.
 */
public class Runner {

    public static final int ERRNO_ERRORS = 1;
    public static final int ERRNO_USAGE  = 2;

    protected static long          mBeginTime;
    protected        Configuration config;

    public static void gradleRun(InputParam inputParam) {
        mBeginTime = System.currentTimeMillis();
        Runner m = new Runner();
        m.run(inputParam);
    }

    private void run(InputParam inputParam) {
        loadConfigFromGradle(inputParam);
        try {
            Logger.initLogger(config);
            tinkerPatch();
        } catch (IOException e) {
            e.printStackTrace();
            goToError();
        } finally {
            Logger.closeLogger();
        }
    }

    protected void tinkerPatch() {
        Logger.d("-----------------------Tinker patch begin-----------------------");

        Logger.d(config.toString());
        try {
            //gen patch
            ApkDecoder decoder = new ApkDecoder(config);
            decoder.onAllPatchesStart();
//            MARK 会先对manifest文件进行检测，看其是否有更改，如果发现manifest的组件有新增，则抛出异常，因为目前Tinker暂不支持四大组件的新增。
//            检测通过后解压apk文件，遍历新旧apk，交给ApkFilesVisitor进行处理。
            decoder.patch(config.mOldApkFile, config.mNewApkFile);
            decoder.onAllPatchesEnd();

            //gen meta file and version file
            PatchInfo info = new PatchInfo(config);
            info.gen();

            //build patch
            PatchBuilder builder = new PatchBuilder(config);
            builder.buildPatch();

        } catch (Throwable e) {
            e.printStackTrace();
            goToError();
        }

        Logger.d("Tinker patch done, total time cost: %fs", diffTimeFromBegin());
        Logger.d("Tinker patch done, you can go to file to find the output %s", config.mOutFolder);
        Logger.d("-----------------------Tinker patch end-------------------------");
    }

    private void loadConfigFromGradle(InputParam inputParam) {
        try {
            config = new Configuration(inputParam);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TinkerPatchException e) {
            e.printStackTrace();
        }
    }

    public void goToError() {
        System.exit(ERRNO_USAGE);
    }

    public double diffTimeFromBegin() {
        long end = System.currentTimeMillis();
        return (end - mBeginTime) / 1000.0;
    }

}
