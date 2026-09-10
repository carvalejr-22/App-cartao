from pathlib import Path

activity = Path('app/src/main/java/com/carlos/appcartao/ModernCardActivity.kt')
text = activity.read_text(encoding='utf-8')

# Compact vector icons: avoid emoji glyphs and keep actions on the same line as the amount.
text = text.replace(
    'import androidx.compose.foundation.background\n',
    'import androidx.compose.foundation.background\nimport androidx.compose.foundation.clickable\n',
    1,
)
text = text.replace(
    'import androidx.compose.material3.HorizontalDivider\nimport androidx.compose.material3.IconButton\n',
    'import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.outlined.DeleteOutline\nimport androidx.compose.material.icons.outlined.Edit\nimport androidx.compose.material3.HorizontalDivider\nimport androidx.compose.material3.Icon\n',
    1,
)

old_purchase_actions = '''                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatModernMoney(purchase.amountCents), fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { pendingEdit = purchase }) {
                                    Text("✎", fontSize = 21.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                                IconButton(onClick = { pendingDelete = purchase }) {
                                    Text("🗑", fontSize = 19.sp, color = Color(0xFFB42318))
                                }
                            }
                        }
'''
new_purchase_actions = '''                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(formatModernMoney(purchase.amountCents), fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable { pendingEdit = purchase },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Editar valor",
                                    tint = Color(0xFF667085),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable { pendingDelete = purchase },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Excluir compra",
                                    tint = Color(0xFFB42318),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
'''
if old_purchase_actions not in text:
    raise SystemExit('Purchase action block not found')
text = text.replace(old_purchase_actions, new_purchase_actions, 1)

old_category_delete = '''                                IconButton(onClick = {
                                    val updated = data.categories.filterNot { it == category }
                                    onCategoriesChanged(if (updated.isEmpty()) listOf("Outros") else updated)
                                }) {
                                    Text("🗑", fontSize = 18.sp, color = Color(0xFFB42318))
                                }
'''
new_category_delete = '''                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable {
                                            val updated = data.categories.filterNot { it == category }
                                            onCategoriesChanged(if (updated.isEmpty()) listOf("Outros") else updated)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Excluir categoria",
                                        tint = Color(0xFFB42318),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
'''
if old_category_delete not in text:
    raise SystemExit('Category delete block not found')
text = text.replace(old_category_delete, new_category_delete, 1)
activity.write_text(text, encoding='utf-8')

build = Path('app/build.gradle.kts')
build_text = build.read_text(encoding='utf-8')
build_text = build_text.replace('versionCode = 8', 'versionCode = 9', 1)
build_text = build_text.replace('versionName = "0.6.0"', 'versionName = "0.6.1"', 1)
if 'androidx.compose.material:material-icons-extended' not in build_text:
    build_text = build_text.replace(
        '    implementation("androidx.compose.material3:material3")\n',
        '    implementation("androidx.compose.material3:material3")\n    implementation("androidx.compose.material:material-icons-extended")\n',
        1,
    )
build.write_text(build_text, encoding='utf-8')

workflow = Path('.github/workflows/build-apk.yml')
workflow_text = workflow.read_text(encoding='utf-8')
workflow_text = workflow_text.replace('App-cartao-v0.6.0', 'App-cartao-v0.6.1')
workflow.write_text(workflow_text, encoding='utf-8')
