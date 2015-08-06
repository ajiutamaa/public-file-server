package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import play.libs.ws.WSClient;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.F.*;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by lenovo on 7/27/2015.
 */
public class FileServiceServer extends Controller{
    static final String STORAGE_ENDPOINT = "http://192.168.10.188:9009/img";

    @Inject
    WSClient ws;

    JsonNodeFactory factory = JsonNodeFactory.instance;

    // allow CORS
    public Result preflight(String all) {
        response().setHeader("Access-Control-Allow-Origin", "*");
        response().setHeader("Allow", "*");
        response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
        response().setHeader("Access-Control-Allow-Headers", "Origin, X-AUTH-TOKEN, X-Requested-With, Content-Type, Accept, Referer, User-Agent");
        return ok();
    }

    public Promise<Result> checkAvailability() {
        return StorageUtils.isStorageAvailable().map(new Function<Boolean, Result>() {
            @Override
            public Result apply(Boolean status) throws Throwable {
                if (status) return ok("available");
                else return status(406, "not available");
            }
        });
    }

    public Result getStorageUrl(String farmerId, String meta) {
        ObjectNode node = factory.objectNode();
        node.put("url", StorageUtils.generateFileUrl(farmerId, meta));
        return ok(node);
    }

    public Result upload(String farmerId, String fileMeta, String identifier) {
        if (SecurityUtils.checkIdentifier(identifier, farmerId, fileMeta)) {
            try {
                Http.MultipartFormData body = request().body().asMultipartFormData();
                Http.MultipartFormData.FilePart filepart = body.getFile("image");
                File imgFile = filepart.getFile();
                String filePath = "/"+farmerId+"/"+fileMeta;
                String fileName = filepart.getFilename();
                String fileUrl = StorageUtils.storeFile(filePath, imgFile, fileName);
                if (fileUrl == null) {
                    return internalServerError("error storing file");
                } else {
                    ws.url("http://localhost:9000/farmer/upload/storeUrl")
                            .setQueryParameter("farmer_id", SecurityUtils.decodeToString(farmerId))
                            .setQueryParameter("file_info", SecurityUtils.decodeToString(fileMeta))
                            .setQueryParameter("file_path", STORAGE_ENDPOINT + fileUrl)
                            .get().get(5000);
                    ObjectNode node = factory.objectNode();
                    node.put("public_url", STORAGE_ENDPOINT+fileUrl);
                    return ok(node);
                }
            } catch (Exception e) {
                return internalServerError("IO Error");
            }
        } else {
            return unauthorized("invalid url");
        }
    }

    public Result deleteFarmer (String farmerId) {
        if (StorageUtils.deleteFarmerDirectory(farmerId)) {
            return ok("farmer directory deleted");
        } else {
            return internalServerError("internal error occured");
        }
    }

    public Promise<Result> getImage(String farmerId, String fileMeta, String fileName) {
        try {
            String filePath = "public/"+farmerId+"/"+fileMeta+"/"+fileName;
            Path path = Paths.get(filePath);
            File file = path.toFile();
            if (!file.exists()) return Promise.promise(new Function0<Result>() {
                @Override
                public Result apply() throws Throwable {
                    return internalServerError("file does not exist");
                }
            });
            response().setContentType(Files.probeContentType(path));
            return StorageUtils.getFileBytes(file).map(new F.Function<byte[], Result>() {
                @Override
                public Result apply(byte[] bytes) throws Throwable {
                    return ok(bytes);
                }
            });
        } catch (Exception e) {
            return Promise.promise(new Function0<Result>() {
                @Override
                public Result apply() throws Throwable {
                    return internalServerError("internal error");
                }
            });
        }
    }

    public Promise<Result> deleteImage(String farmerId, String fileMeta, String fileName) {
        try {
            String filePath = "public/"+farmerId+"/"+fileMeta+"/"+fileName;
            Path path = Paths.get(filePath);
            File file = path.toFile();
            return StorageUtils.deleteFarmerFile(file)
                    .map(new Function<Boolean, Result>() {
                        @Override
                        public Result apply(Boolean status) throws Throwable {
                            return status? ok("deleted") : internalServerError("internal error");
                        }
                    });
        } catch (Exception e) {
            return Promise.promise(new Function0<Result>() {
                @Override
                public Result apply() throws Throwable {
                    return internalServerError("internal error");
                }
            });
        }
    }
}
