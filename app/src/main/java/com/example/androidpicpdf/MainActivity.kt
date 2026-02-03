package com.example.androidpicpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.androidpicpdf.data.AppDatabase
import com.example.androidpicpdf.data.ProjectRepository
import com.example.androidpicpdf.ui.ProjectEditorViewModel
import com.example.androidpicpdf.ui.ProjectListViewModel
import com.example.androidpicpdf.ui.screens.ExportScreen
import com.example.androidpicpdf.ui.screens.ProjectEditorScreen
import com.example.androidpicpdf.ui.screens.ProjectListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AppNavHost()
                }
            }
        }
    }
}

@Composable
private fun AppNavHost() {
    val context = LocalContext.current
    val database = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "projects.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    val repository = remember { ProjectRepository(database.projectDao()) }
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "projects") {
        composable("projects") {
            val viewModel: ProjectListViewModel = viewModel(factory = simpleFactory { ProjectListViewModel(repository) })
            ProjectListScreen(
                viewModel = viewModel,
                onOpenProject = { projectId -> navController.navigate("editor/$projectId") }
            )
        }
        composable(
            route = "editor/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { entry ->
            val projectId = entry.arguments?.getLong("projectId") ?: return@composable
            val viewModel: ProjectEditorViewModel = viewModel(
                factory = simpleFactory { ProjectEditorViewModel(repository, projectId) }
            )
            ProjectEditorScreen(
                projectId = projectId,
                repository = repository,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onExport = { navController.navigate("export/$projectId") }
            )
        }
        composable(
            route = "export/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { entry ->
            val projectId = entry.arguments?.getLong("projectId") ?: return@composable
            ExportScreen(
                projectId = projectId,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun <T : ViewModel> simpleFactory(create: () -> T): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            @Suppress("UNCHECKED_CAST")
            return create() as VM
        }
    }
}
