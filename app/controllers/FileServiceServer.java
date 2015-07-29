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
    @Inject
    WSClient ws;

    JsonNodeFactory factory = JsonNodeFactory.instance;

    public Promise<Result> checkAvailability() {
        return StorageUtils.isStorageAvailable().map(new Function<Boolean, Result>() {
            @Override
            public Result apply(Boolean status) throws Throwable {
                if (status) return ok("available");
                else return status(406, "not available");
            }
        });
    }

    public Result getStorageUrl(int farmerId, String meta) {
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
                    storeUrlWsCall(fileUrl);
                    ObjectNode node = factory.objectNode();
                    node.put("public_url", fileUrl);
                    return ok(node);
                }
            } catch (Exception e) {
                return internalServerError("IO Error");
            }
        } else {
            return unauthorized("invalid url");
        }
    }

    private Boolean storeUrlWsCall(String url) {
        if (ws.url("http://localhost:9000/upload/storeUrl")
                .setQueryParameter("file_path", url)
                .get().map(new Function<WSResponse, Boolean>() {
            @Override
            public Boolean apply(WSResponse response) throws Throwable {
                return response.getStatus() == OK;
            }
        }).get(5000)){
            return true;
        } else {
            return storeUrlWsCall(url);
        }
    }

    public Promise<Result> getImage(String farmerId, String fileMeta, String fileName) {
        try {
            String filePath = "public/"+farmerId+"/"+fileMeta+"/"+fileName;
            Path path = Paths.get(filePath);
            File file = path.toFile();
            response().setContentType(Files.probeContentType(path));
            return StorageUtils.getFileBytes(file).map(new F.Function<byte[], Result>() {
                @Override
                public Result apply(byte[] bytes) throws Throwable {
                    return ok(bytes);
                }
            });
        } catch (IOException e) {
            return Promise.promise(new Function0<Result>() {
                @Override
                public Result apply() throws Throwable {
                    return internalServerError("internal error");
                }
            });
        }
    }
}
