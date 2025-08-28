package com.example.litertmobilenetssample

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView // ★ ImageViewをインポート
import android.widget.TextView
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var resultTextView: TextView
    private lateinit var sampleImageView: ImageView // ★ ImageViewのプロパティを追加

    private var labels: List<String> = emptyList()
    private var imageClassifier: ImageClassifier? = null

    // --- 仮定するファイル名 (もし違う場合はここを修正してください) ---
    private val sampleImageNameInDrawable = "sample_image" // あなたのdrawableフォルダ内のサンプル画像名 (拡張子なし)
    private val tfliteModelNameInAssets = "mobilenet_v3_1.0_224_quant.tflite" // あなたのassetsフォルダ内のモデルファイル名
    private val labelsFileNameInAssets = "labels_JP.txt"        // あなたのassetsフォルダ内のラベルファイル名
    // --- 仮定ここまで ---

    private val logTag = "TFLiteDemo_MainActivity" // Logcat用のタグ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // activity_main.xml をセット

        resultTextView = findViewById(R.id.result_text) // レイアウト内のTextViewのID
        sampleImageView = findViewById(R.id.image_view_sample) // ★ ImageViewの参照を取得

        resultTextView.text = "Initializing App...\n" // 初期メッセージを少し変更

        // 1. ラベルファイルをロード
        loadLabels()

        // 2. ラベルがロードできたら、分類器をセットアップし、画像を処理
        if (labels.isNotEmpty()) {
            setupImageClassifier()
            processSampleImage()
        } else {
            // loadLabels内でエラーメッセージは表示済みのはず
            Log.w(logTag, "Labels not loaded. Cannot proceed with classification setup.")
        }
    }

    private fun loadLabels() {
        try {
            labels = FileUtil.loadLabels(this, labelsFileNameInAssets)
            val successMsg = "Labels loaded: ${labels.size} labels found.\n"
            resultTextView.append(successMsg)
            Log.i(logTag, successMsg.trim())
        } catch (e: IOException) {
            handleError("Error loading '$labelsFileNameInAssets': ${e.message}", e)
        }
    }

    private fun setupImageClassifier() {
        if (imageClassifier != null) {
            Log.i(logTag, "ImageClassifier instance already exists.")
            return
        }
        try {
            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(5)
                .setNumThreads(4)
                .build()

            imageClassifier = ImageClassifier.createFromFileAndOptions(
                this,
                tfliteModelNameInAssets,
                options
            )
            val successMsg = "ImageClassifier initialized successfully with '$tfliteModelNameInAssets'.\n"
            resultTextView.append(successMsg)
            Log.i(logTag, successMsg.trim())

        } catch (e: Exception) {
            handleError("TFLite failed to load model '$tfliteModelNameInAssets': ${e.message}", e)
        }
    }

    private fun processSampleImage() {
        if (imageClassifier == null) {
            val errorMsg = "Classifier not initialized. Cannot process image.\n"
            resultTextView.append(errorMsg)
            Log.e(logTag, errorMsg.trim())
            return
        }

        // loadBitmapFromDrawableメソッド内でImageViewへのセットが行われる
        val bitmap: Bitmap? = loadBitmapFromDrawable(sampleImageNameInDrawable)

        if (bitmap != null) {
            // ImageViewへのセットはloadBitmapFromDrawable内で行われたので、ここでは分類処理のみ
            val infoMsg = "Sample image '$sampleImageNameInDrawable' loaded. Classifying...\n"
            // resultTextViewへのメッセージ追加は残してもOK (画像ロード成功の旨)
            // resultTextView.append(infoMsg) // この行は重複する可能性があるのでコメントアウトまたは削除を検討
            Log.i(logTag, "${infoMsg.trim()} (Width: ${bitmap.width}, Height: ${bitmap.height})")
            classifyImage(bitmap)
        } else {
            Log.w(logTag, "Sample image not loaded. Classification skipped.")
        }
    }

    private fun loadBitmapFromDrawable(imageName: String): Bitmap? {
        val msgPrefix = "Loading bitmap '$imageName'"
        return try {
            val resourceId = resources.getIdentifier(imageName, "drawable", packageName)
            if (resourceId == 0) {
                throw IOException("Resource ID for '$imageName' not found in drawable. Check filename and placement.")
            }
            Log.d(logTag, "$msgPrefix: Resource ID $resourceId found.")
            val bitmap = BitmapFactory.decodeResource(resources, resourceId)

            // ★★★ BitmapをImageViewに設定する処理 ★★★
            if (bitmap != null) {
                // UIスレッドでImageViewを更新
                runOnUiThread {
                    sampleImageView.setImageBitmap(bitmap)
                }
                Log.i(logTag, "Bitmap set to ImageView successfully for '$imageName'.")
                resultTextView.append("Displaying image: $imageName\n") // 画像表示の旨をTextViewにも追記
            } else {
                Log.w(logTag, "Bitmap is null for '$imageName', cannot set to ImageView.")
                resultTextView.append("Failed to load image: $imageName (Bitmap is null)\n")
            }
            // ★★★ ここまで ★★★

            bitmap // ロードされたbitmapを返す
        } catch (e: Exception) {
            handleError("$msgPrefix error: ${e.message}", e)
            null
        }
    }

    private fun classifyImage(bitmap: Bitmap) {
        if (imageClassifier == null) {
            Log.e(logTag, "classifyImage called but imageClassifier is null.")
            resultTextView.append("Error: Classifier became null unexpectedly.\n")
            return
        }
        try {
            Log.d(logTag, "Preparing TensorImage from Bitmap...")
            val tensorImage = TensorImage.fromBitmap(bitmap)

            Log.d(logTag, "Starting classification...")
            val startTime = System.currentTimeMillis()
            val results: List<Classifications>? = imageClassifier?.classify(tensorImage)
            val endTime = System.currentTimeMillis()
            Log.i(logTag, "Classification inference time: ${endTime - startTime} ms")

            displayClassificationResults(results)

        } catch (e: Exception) {
            handleError("Error during classification: ${e.message}", e)
        }
    }

    private fun displayClassificationResults(classificationsList: List<Classifications>?) {
        resultTextView.append("\n--- Classification Results ---\n")
        classificationsList?.let {
            if (it.isNotEmpty() && it[0].categories.isNotEmpty()) {
                it[0].categories.forEachIndexed { resultIndex, category ->

                    Log.d(logTag, "Raw Category Data for result ${resultIndex + 1}: " +
                            "Index=${category.index}, " +
                            "Score=${category.score}, " +
                            "LabelFromModel='${category.label}', " +
                            "DisplayName='${category.displayName}'")

                    val labelName = if (category.index >= 0 && category.index < labels.size) {
                        labels[category.index]
                    } else {
                        if (category.displayName.isNotEmpty()) {
                            category.displayName
                        } else {
                            "Unknown Label (Index: ${category.index})"
                        }
                    }

                    val rawScore = category.score
                    var displayText: String

                    val assumedMaxRawScore = 0.3f // ★ 傾向を見て調整（V2=15.0f）
                    var scaledScorePercent = (rawScore / assumedMaxRawScore) * 100.0f
                    scaledScorePercent = scaledScorePercent.coerceIn(0.0f, 100.0f)
                    displayText = "${String.format("%.1f", scaledScorePercent)}%"

                    val textToShow = "${resultIndex + 1}. $labelName: $displayText\n"
                    resultTextView.append(textToShow)
                    Log.i(logTag, textToShow.trim())
                }
            } else {
                val noResultsMsg = "No classification results found or categories are empty.\n"
                resultTextView.append(noResultsMsg)
                Log.i(logTag, noResultsMsg.trim())
            }
        } ?: run {
            val nullResultsMsg = "Classification returned null or empty results.\n"
            resultTextView.append(nullResultsMsg)
            Log.i(logTag, nullResultsMsg.trim())
        }
        resultTextView.append("----------------------------\n")
    }

    private fun handleError(errorMessage: String, exception: Throwable? = null) {
        val fullErrorMessage = "ERROR: $errorMessage\n"
        resultTextView.append(fullErrorMessage)
        if (exception != null) {
            Log.e(logTag, errorMessage, exception)
        } else {
            Log.e(logTag, errorMessage)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(logTag, "onDestroy called. Closing ImageClassifier.")
        try {
            imageClassifier?.close()
        } catch (e: Exception) {
            Log.e(logTag, "Error closing ImageClassifier.", e)
        }
        imageClassifier = null
    }
}
