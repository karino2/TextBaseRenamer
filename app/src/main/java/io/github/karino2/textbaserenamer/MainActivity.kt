package io.github.karino2.textbaserenamer

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import io.github.karino2.textbaserenamer.ui.theme.TextBaseRenamerTheme

class MainActivity : ComponentActivity() {
    var targetUri : Uri? = null

    val getNewDirectory = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { url1->
        url1?.let {
            targetUri = it
            updateDir(it)
        }
    }

    val leftNames = mutableStateOf("Left Names")
    val rightNames = mutableStateOf("Right Names")

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("TARGET_URI", targetUri.toString())
        outState.putString("LEFT_NAMES", leftNames.value)
        outState.putString("RIGHT_NAMES", rightNames.value)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        targetUri = Uri.parse(savedInstanceState.getString("TARGET_URI"))
        leftNames.value = savedInstanceState.getString("LEFT_NAMES", "")
        leftNames.value = savedInstanceState.getString("LEFT_NAMES", "")
        super.onRestoreInstanceState(savedInstanceState)
    }


    fun updateDir(dirUri: Uri) {
        val files = listFiles(dirUri)
            .map { it.name }
            .joinToString("\n")

        leftNames.value = files
        rightNames.value = files
    }

    private fun listFiles(dirUri: Uri) = FastFile.fromTreeUri(this, dirUri)
        .listFiles()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TextBaseRenamerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column()
                    {
                        TopAppBar(title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { doRenames() }) {
                                    Icon(Icons.Filled.Done, contentDescription = "Run")
                                }
                              }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                        TwoPane(leftNames.value, rightNames.value, { leftNames.value = it }, { rightNames.value = it })
                    }
                }
            }
        }

        if (targetUri == null)
        {
            getNewDirectory.launch(null)
            return
        }
    }

    fun toList(text: String) : List<String> {
        return text.split("\n")
            .filter { it != ""}
    }

    fun doRenames() {
        val lefts = toList(leftNames.value)
        val rights = toList(rightNames.value)

        if (lefts.size != rights.size) {
            showMessage("left and right line num differ: ${lefts.size}, ${rights.size}")
            return
        }

        val fileMap = listFiles(targetUri!!)
                        .map { Pair(it.name, it) }
                        .toMap()

        lefts.zip(rights)
            .forEach { (left, right)->
                if (left != right) {
                    val f = fileMap[left]
                    f?.renameTo(right) ?: showMessage("Left name (${left}) does not exist or fail to rename.")
                }
            }
        clearContent()
        showMessage("Rename done.")
    }

    fun clearContent() {
        leftNames.value = ""
        rightNames.value = ""
        targetUri = null
    }

    fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

}


@Composable
fun TwoPane(textBefore: String, textAfter: String, onBeforeTextChanged: (String)->Unit, onAfterTextChanged: (String)->Unit) {
    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE-> {
            Row(Modifier.fillMaxSize()) {
                TextField( value = textBefore, onValueChange = onBeforeTextChanged, label= { Text("Before") }, modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight())
                Divider(color = Color.Black, modifier = Modifier.fillMaxHeight().width(2.dp), thickness = 2.dp)
                TextField( value = textAfter, onValueChange = onAfterTextChanged, label= { Text("After") }, modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight() )
            }
        }
        else -> {
            Column(Modifier.fillMaxSize()) {
                TextField( value = textBefore, onValueChange = onBeforeTextChanged, label= { Text("Before") }, modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth())
                Divider(color = Color.Black, modifier = Modifier.fillMaxWidth().height(2.dp), thickness = 2.dp)
                TextField( value = textAfter, onValueChange = onAfterTextChanged, label= { Text("After") }, modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth() )
            }
        }
    }
}
