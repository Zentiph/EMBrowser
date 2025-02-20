package downloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.util.HashSet;


public class YouTubeDownloader {
    private static Process flaskServerProcess;
    private static boolean serverRunning = false;
    private static String apiUrlBase = "http://127.0.0.1:5000/download?url=";

    /**
     * Run this downloader with command-line arguments.
     *
     * @param args - CL args
     */
    public static void main(String[] args) {
        String url = null;
        boolean urlParsed = false;

        HashSet<VideoType> videoTypes = new HashSet<>();
        HashSet<String> dirs = new HashSet<>();
        HashSet<String> filenames = new HashSet<>();

        // Parse args
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-url":
                    if (urlParsed) {
                        System.out.println("Error: Duplicate argument for -url");
                        printUsage();
                        break;
                    }

                    if (i + 1 < args.length) {
                        url = args[i + 1];
                        i++;
                    } else {
                        System.out.println("Error: Missing value for -url");
                        printUsage();
                        return;
                    }
                    break;

                case "-vt":
                case "--video-type":
                    if (i + 1 < args.length) {
                        videoTypes.add(VideoType.fromString(args[i + 1]));
                        i++;
                    } else {
                        System.out.println("Error: Missing value for --video-type or -vt");
                        printUsage();
                        return;
                    }
                    break;

                case "-dir":
                    if (i + 1 < args.length) {
                        dirs.add(args[i + 1]);
                        i++;
                    } else {
                        System.out.println("Error: Missing value for -dir");
                        printUsage();
                        return;
                    }
                    break;

                case "-fn":
                case "--filename":
                    if (i + 1 < args.length) {
                        filenames.add(args[i + 1]);
                        i++;
                    } else {
                        System.out.println("Error: Missing value for --filename or -fn");
                        printUsage();
                        return;
                    }
                    break;

                default:
                    System.out.println("Error: Unexpected argument given: " + args[i]);
                    printUsage();
                    return;
            }
        }

        // Validate a URL was given
        if (url == null) {
            System.out.println("Error: Missing URL argument");
            printUsage();
            return;
        }

        // If the video types, filenames, or dirs
        // are still empty, add defaults
        if (videoTypes.size() == 0) {
            videoTypes.add(VideoType.BOTH);
        }
        if (filenames.size() == 0) {
            filenames.add("emb_" + LocalDateTime.now().toString());
        }
        if (dirs.size() == 0) {
            dirs.add(null); // Will be changed to CWD later
        }

        YouTubeDownloader.startFlaskServer();

        for (VideoType vt : videoTypes) {
            for (String dir : dirs) {
                for (String filename : filenames) {
                    YouTubeDownloader.download(url, dir, filename, vt);
                }
            }
        }

        YouTubeDownloader.stopFlaskServer();
    }

    /**
     * Download this video.
     * Note this method does not start or stop the Flask server.
     * To do so, use the {@link #downloadAndEnd(String)} method
     *
     * @param url - URL of the YouTube video
     * @param dir - Path to the output directory;
     * will use the current working directory if null
     * @param filename - Path to the output file
     * @param videoType - Type of video to download
     */
    public static void download(String url, String dir, String filename, VideoType videoType) {
        if (!serverRunning) throw new IllegalStateException(
            "Cannot download video if Flask server is offline"
        );

        if (dir == null) {
            // Get CWD
            dir = FileSystems.getDefault().getPath(".").toAbsolutePath().toString();
        }

        // Charset.defaultCharset() is UTF-8
        String encodedURL = URLEncoder.encode(url, Charset.defaultCharset());
        String encodedDir = URLEncoder.encode(dir, Charset.defaultCharset());
        String encodedFilename = URLEncoder.encode(filename, Charset.defaultCharset());
        String encodedVideoType = URLEncoder.encode(videoType.asUrlArg(), Charset.defaultCharset());

        String apiUrl = apiUrlBase + encodedURL + "&type=" + encodedVideoType
            + "&dir=" + encodedDir + "&fn=" + encodedFilename;

        makeRequest(apiUrl);
    }

    /**
     * Check whether the Flask server is currently running.
     *
     * @return Whether the Flask server is running
     */
    public boolean flaskServerRunning() {
        return serverRunning;
    }

    /**
     * Start the Flask server.
     */
    public static void startFlaskServer() {
        if (serverRunning) return;

        try {
            String command = "python src/downloader/app.py";
            flaskServerProcess = new ProcessBuilder(command.split(" ")).start();

            serverRunning = true;
        } catch (IOException e) {
            // TODO need better handling at some point
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stop the Flask server.
     */
    public static void stopFlaskServer() {
        if (!serverRunning) return;

        try {
            if (flaskServerProcess != null) {
                flaskServerProcess.destroy();

                serverRunning = false;
            }
        } catch (Exception e) {
                // TODO need better handling at some point
                System.out.println("Exception: " + e.getMessage());
                e.printStackTrace();
        }
    }

    /**
     * Make a request to the Flask server.
     *
     * @param apiUrl - API URL to use in the request
     */
    private static void makeRequest(String apiUrl) {
        try {
            URL url = new URI(apiUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // TODO
            // Print response for now, remove at some point
            // Maybe return?
            System.out.println(response.toString());
        } catch (Exception e) {
            // TODO need better handling at some point
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java YouTubeDownloader {-url} {--video-type} (named) {-vt} (short) {--filename} (named) {-fn} (short)");
        System.out.println();
        System.out.println("  {-url}         - The URL of the YouTube video");
        System.out.println("                   (required: yes; duplicates: no)");
        System.out.println("  {--video-type} - The type of the video to download");
        System.out.println("                   (required: no; options: 'both', 'video', 'audio'; default: 'both'; duplicates: yes)");
        System.out.println("  {-vt}          - Shortcut for --video-type");
        System.out.println("  {-dir}         - The directory where the video will be saved (absolute path)");
        System.out.println("                   (required: no; default: the current directory ('.'); duplicates: yes)");
        System.out.println("  {--filename}   - The name of the file");
        System.out.println("                   (required: no; default: 'emb_' + current time string))");
        System.out.println("  {-fn}          - Shortcut for --filename");
    }
}