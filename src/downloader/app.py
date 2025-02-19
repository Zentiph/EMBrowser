"""Main file for the downloader."""

from flask import Flask, request, jsonify
from pytube import YouTube  # type: ignore


class InvalidVideoTypeError(Exception):
    """Exception to raise when an invalid video type was passed.
    Valid video types are: "video", "audio", "mp4", "mov", "mp3"
    """


app = Flask(__name__)


@app.route("/download", methods=["GET"])
def download_video():
    """Download the given YouTube video.

    :return: The JSON response to the request and the HTTP code.
    :rtype: Tuple[flask.Response, Literal[200]] | Tuple[flask.Response, Literal[400]]
    """

    video_url = request.args.get("url")
    video_type = request.args.get("type")
    filepath = request.args.get("fp")

    try:
        yt = YouTube(video_url)

        match video_type:
            case "video":
                stream = yt.streams.filter(only_video=True).get_highest_resolution()
            case "audio", "mp3":
                stream = yt.streams.filter(only_audio=True).first()
            case "mp4":
                stream = yt.streams.filter(
                    file_extension="mp4"
                ).get_highest_resolution()
            case "mov":
                stream = yt.streams.filter(
                    file_extension="mov"
                ).get_highest_resolution()
            case _:
                raise InvalidVideoTypeError(
                    f"invalid video type given: {repr(video_type)}; "
                    + "expected 'video', 'audio', 'mp4', 'mov', or 'mp3'"
                )

        stream.download(filepath)
        return jsonify({"message": "Download successful!"}), 200

    except Exception as e:  # pylint: disable=broad-exception-caught
        return jsonify({"error": str(e)}), 400


if __name__ == "__main__":
    app.run(debug=True)
