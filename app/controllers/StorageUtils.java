package controllers;

import org.apache.commons.io.FileUtils;
import play.Logger;
import play.libs.F;
import play.mvc.Controller;
import play.libs.F.Promise;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * Created by lenovo on 7/27/2015.
 */
public class StorageUtils extends Controller{

    final static long MAX_IMAGE_SIZE = 1L * 1024L * 1024L;
    final static long MAX_DIFF = 50L * 1024L * 1024L;

    public Result getStorageFreeSpace() {
        return ok("Free space: "+ ((new File("/").getUsableSpace()) - MAX_IMAGE_SIZE));
    }

    public static Promise<Boolean> isStorageAvailable() {
        return Promise.promise(new F.Function0<Boolean>() {
            @Override
            public Boolean apply() throws Throwable {
                long freeSpace = new File("/").getFreeSpace();
                double diff = freeSpace - MAX_IMAGE_SIZE;
                boolean available = (freeSpace - MAX_IMAGE_SIZE) > MAX_DIFF;
                return available;
            }
        });
    }

    public static String generateFileUrl(String farmerId, String meta) {
        String encodedId = SecurityUtils.encodeToString(farmerId);
        String encodedMeta = SecurityUtils.encodeToString(meta);
        String secret = SecurityUtils.createSecret(encodedId, encodedMeta);
        String identifier = SecurityUtils.encodeToString(secret);
        String genUrl = "/"+encodedId+"/"+encodedMeta+"/"+identifier;
        return genUrl;
    }

    public static String storeFile (String path, File image, String name) {
        try {
            String timestamp = new Date().toString();
            String randFileName = SecurityUtils.encodeToString(path)
                    .substring(path.length()-5,path.length()) +
                    timestamp;
            int startExt = name.lastIndexOf(".");
            String extension = name.substring(startExt, name.length());
            File newFile = new File("public" + path, randFileName+extension);
            String dirName = "public" + path;
            FileUtils.deleteDirectory(new File(dirName));
            FileUtils.moveFile(image, newFile);
            return path+"/"+randFileName+extension;
        } catch (IOException ioex){
            return null;
        }
    }

    public static boolean deleteFarmerDirectory (String farmerId) {
        try {
            FileUtils.deleteDirectory(new File("public/"+SecurityUtils.encodeToString(farmerId)));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static F.Promise<Boolean> deleteFarmerFile (final File file) {
        return F.Promise.promise(new F.Function0<Boolean>() {
            @Override
            public Boolean apply() throws Throwable {
                FileUtils.deleteQuietly(file);
                return true;
            }
        });
    }

    public static F.Promise<byte[]> getFileBytes(final File file) {
        return F.Promise.promise(new F.Function0<byte[]>() {
            @Override
            public byte[] apply() throws Throwable {
                byte[] buffer = new byte[1024];
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                FileInputStream is = new FileInputStream(file);

                for (int readNum; (readNum = is.read(buffer)) != -1; ) {
                    os.write(buffer, 0, readNum);
                }
                return os.toByteArray();
            }
        });
    }
}
