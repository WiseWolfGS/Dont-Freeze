package net.WWGS.dontfreeze;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class BlueprintPackInstaller {
    private BlueprintPackInstaller() {}

    private static final String PACK_ROOT = "DontFreeze";
    private static final String RESOURCE_ROOT = "/blueprints/" + PACK_ROOT + "/";

    // JAR 안에 넣어둔 파일 목록(필요한 만큼 추가)
    private static final String[] FILES = {
            "pack.json",
            "generator/generator1.blueprint",
            "icon.png"
    };

    public static void ensureInstalled() {
        Path gameDir = FMLPaths.GAMEDIR.get();
        Path targetRoot = gameDir.resolve("blueprints").resolve(PACK_ROOT);

        try {
            int bundledVer = readBundledPackVersion();
            int installedVer = readInstalledPackVersion(targetRoot.resolve("pack.json"));

            // 없거나(0), 구버전이면 덮어쓰기
            if (installedVer < bundledVer) {
                copyAll(targetRoot);
            }
        } catch (Exception e) {
            // 여기서 LOGGER로 찍어두면 디버깅 편함
            // DontFreeze.LOGGER.error("Failed to install MineColonies blueprint pack", e);
        }
    }

    private static int readBundledPackVersion() throws IOException {
        try (InputStream in = BlueprintPackInstaller.class.getResourceAsStream(RESOURCE_ROOT + "pack.json")) {
            if (in == null) return 0;
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.has("version") ? obj.get("version").getAsInt() : 0;
        }
    }

    private static int readInstalledPackVersion(Path installedPackJson) {
        try {
            if (!Files.exists(installedPackJson)) return 0;
            String json = Files.readString(installedPackJson, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.has("version") ? obj.get("version").getAsInt() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static void copyAll(Path targetRoot) throws IOException {
        for (String rel : FILES) {
            copyOne(rel, targetRoot.resolve(rel));
        }
    }

    private static void copyOne(String relPath, Path outPath) throws IOException {
        String resPath = RESOURCE_ROOT + relPath;

        try (InputStream in = BlueprintPackInstaller.class.getResourceAsStream(resPath)) {
            if (in == null) {
                // 리소스 누락: 파일 목록/경로 틀리면 여기 걸림
                return;
            }
            Files.createDirectories(outPath.getParent());
            Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

