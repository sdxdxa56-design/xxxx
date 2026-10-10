package com.appinspector.presentation.github

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appinspector.data.remote.dto.RepoDto
import com.appinspector.data.repository.GitHubRepository
import com.appinspector.presentation.theme.AccentYellow
import com.appinspector.presentation.theme.CrashRed
import com.appinspector.presentation.theme.PrimaryEmerald
import com.appinspector.presentation.theme.SecondaryTeal
import com.appinspector.presentation.theme.SurfaceDark
import com.appinspector.presentation.theme.SurfaceVariantDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListScreen(
    onSelectRepo: (owner: String, repo: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { GitHubRepository(context) }

    var usernameInput by remember { mutableStateOf("" ) }
    var repos by remember { mutableStateOf<List<RepoDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("" ) }
    var showDirectModal by remember { mutableStateOf(false) }
    var directOwner by remember { mutableStateOf("" ) }
    var directRepo by remember { mutableStateOf("" ) }

    val loadRepos = { targetUser: String ->
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                repos = repository.getRepositories(targetUser.ifBlank { null })
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load repositories"
                repos = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRepos("")
    }

    val filteredRepos = repos.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.fullName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub Repositories", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDirectModal = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Direct Input",
                            tint = PrimaryEmerald
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Username or Org", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = PrimaryEmerald,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = PrimaryEmerald
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = { loadRepos(usernameInput) },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text("Load", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search repositories...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryEmerald,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PrimaryEmerald
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Repositories",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryEmerald)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading repositories...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CrashRed.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrashRed, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = CrashRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { loadRepos(usernameInput) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrashRed)
                                ) {
                                    Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                filteredRepos.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No repositories found" else "No matching repositories",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredRepos) { repo ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val parts = repo.fullName.split("/")
                                        if (parts.size == 2) {
                                            onSelectRepo(parts[0], parts[1])
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (repo.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                                        contentDescription = null,
                                        tint = if (repo.isPrivate) AccentYellow else PrimaryEmerald,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = repo.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        if (repo.description != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = repo.description,
                                                color = Color.Gray,
                                                fontSize = 13.sp,
                                                maxLines = 2
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = SurfaceVariantDark
                                            ) {
                                                Text(
                                                    text = repo.defaultBranch ?: "main",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (repo.language != null) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = repo.language,
                                                    color = SecondaryTeal,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDirectModal) {
        AlertDialog(
            onDismissRequest = { showDirectModal = false },
            containerColor = SurfaceDark,
            title = { Text("Open Repository Directly", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter owner and repository name directly (public or private):", color = Color.Gray, fontSize = 13.sp)
                    OutlinedTextField(
                        value = directOwner,
                        onValueChange = { directOwner = it },
                        label = { Text("Owner (e.g. torvalds)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = PrimaryEmerald,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = directRepo,
                        onValueChange = { directRepo = it },
                        label = { Text("Repository name (e.g. linux)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = PrimaryEmerald,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (directOwner.isNotBlank() && directRepo.isNotBlank()) {
                            showDirectModal = false
                            onSelectRepo(directOwner.trim(), directRepo.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text("Open", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectModal = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
