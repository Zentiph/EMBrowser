"""Main file for the downloader."""

from os.path import join

import yt_dlp  # type: ignore
from flask import Flask, jsonify, request
from flask_cors import CORS


class InvalidVideoTypeError(Exception):
    """Exception to raise when an invalid video type was passed.
    Valid video types are: "video-and-audio", "video", "audio"
    """


app = Flask(__name__)
CORS(app)


@app.route("/favicon.ico")
def favicon():
    """Suppress favicon fetch errors."""

    return "", 204


@app.route("/download", methods=["GET"])
def download_video():  # pylint: disable=too-many-return-statements
    """Download the given YouTube video.

    :return: The JSON response to the request and the HTTP code.
    :rtype: Tuple[flask.Response, Literal[200]] | Tuple[flask.Response, Literal[400]]
    """

    video_url = request.args.get("url")
    if not video_url:
        return jsonify({"error": "Missing 'url' parameter"}), 400

    video_type = request.args.get("type")
    if not video_type:
        return jsonify({"error": "Missing 'type' parameter"}), 400

    out_dir = request.args.get("dir")
    if not out_dir:
        return jsonify({"error": "Missing 'dir' parameter"})

    filename = request.args.get("fn")
    if not filename:
        return jsonify({"error": "Missing 'fn' parameter"})

    try:
        yt_dlp_options = {
            "outtmpl": join(out_dir, f"{filename}.%(ext)s"),
            "quiet": True,  # Disable output except for errors
            "reject": ".webm",
        }

        match video_type:
            case "video-and-audio":
                yt_dlp_options["format"] = "bestvideo+bestaudio/best"
            case "video":
                yt_dlp_options["format"] = "bestvideo"
            case "audio":
                yt_dlp_options["format"] = "bestaudio"
            case _:
                raise InvalidVideoTypeError(
                    f"invalid video type given: {repr(video_type)}; "
                    + "expected 'video' or 'audio'"
                )

        with yt_dlp.YoutubeDL(yt_dlp_options) as downloader:
            info = downloader.extract_info(video_url, download=True)
            response = {
                "message": "Download successful!",
                "video_title": info.get("title"),
                "video_url": info.get("url"),
                "video_format": info.get("ext"),
                "video_duration": info.get("duration"),
                "video_size": info.get("filesize"),
            }

        return jsonify(response), 200

    except Exception as e:  # pylint: disable=broad-exception-caught
        return jsonify({"error": type(e).__name__ + str(e)}), 400


if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True)
