from pathlib import Path

p = Path('app/src/main/java/com/carlos/appcartao/ModernCardActivity.kt')
s = p.read_text()

def rep(old, new, count=1):
    global s
    if old not in s:
        raise SystemExit('Missing pattern:\n' + old[:300])
    s = s.replace(old, new, count)

rep('''    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n        store = ModernSecureStore(this)''', '''    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n        // Remove abandoned temporary receipt photos from interrupted camera sessions.\n        runCatching {\n            File(cacheDir, "receipts").listFiles()?.forEach { file ->\n                if (System.currentTimeMillis() - file.lastModified() > 60L * 60L * 1000L) file.delete()\n            }\n        }\n        store = ModernSecureStore(this)''')

rep('''    val backupContext = androidx.compose.ui.platform.LocalContext.current.applicationContext\n    val backupStatus = remember(data.lastModifiedMillis) { AutomaticBackupStatusStore.read(backupContext) }\n    val backupStatusText = when {\n        backupStatus.lastRestoreMillis > 0L && backupStatus.lastRestoreMillis >= backupStatus.lastCompletedMillis ->\n            "Dados restaurados do backup em ${formatAutomaticBackupMoment(backupStatus.lastRestoreMillis)}."\n        backupStatus.pending && backupStatus.lastRequestMillis >= backupStatus.lastChangeMillis && backupStatus.lastRequestMillis > 0L ->\n            "Alteração salva. Pedido enviado ao Android; aguardando a conclusão do backup."\n        backupStatus.pending ->\n            "Alteração salva no aparelho. Backup aguardando conexão com a internet."\n        backupStatus.lastCompletedMillis > 0L ->\n            "Último backup confirmado pelo Android: ${formatAutomaticBackupMoment(backupStatus.lastCompletedMillis)}."\n        else ->\n            "Ainda não há backup confirmado neste aparelho. Faça uma alteração e mantenha o celular conectado à internet."\n    }''', '''    val backupContext = androidx.compose.ui.platform.LocalContext.current.applicationContext\n    val backupStatus = remember(data.lastModifiedMillis) { AutomaticBackupStatusStore.read(backupContext) }\n    val systemBackupEnabled = remember(data.lastModifiedMillis) { AutomaticBackupScheduler.isSystemBackupEnabled(backupContext) }\n    val backupStatusText = when {\n        !systemBackupEnabled ->\n            "O backup do Android está desativado neste aparelho. Ative o backup do sistema para permitir restauração após reinstalação."\n        backupStatus.lastRestoreMillis > 0L && backupStatus.lastRestoreMillis >= backupStatus.lastCompletedMillis ->\n            "Dados restaurados pelo Android em ${formatAutomaticBackupMoment(backupStatus.lastRestoreMillis)}."\n        backupStatus.pending && backupStatus.lastRequestMillis >= backupStatus.lastChangeMillis && backupStatus.lastRequestMillis > 0L ->\n            "Alteração salva. Pedido de backup entregue ao Android; o sistema concluirá o envio em segundo plano."\n        backupStatus.pending ->\n            "Alteração salva no aparelho. O pedido de backup está aguardando conexão com a internet."\n        backupStatus.lastCompletedMillis > 0L ->\n            "Dados entregues ao serviço de backup do Android em ${formatAutomaticBackupMoment(backupStatus.lastCompletedMillis)}."\n        else ->\n            "Ainda não houve uma execução de backup registrada neste aparelho."\n    }''')

rep('''                            Text("✓ Proteção automática", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)''', '''                            Text(\n                                if (systemBackupEnabled) "✓ Proteção automática ativa" else "⚠ Backup do Android desativado",\n                                fontWeight = FontWeight.SemiBold,\n                                color = if (systemBackupEnabled) MaterialTheme.colorScheme.primary else Color(0xFFB54708)\n                            )''')

rep('''                                "O Android decide o instante exato do envio. Só desinstale para testar quando aparecer ‘Último backup confirmado’.",''', '''                                "O app agrupa alterações por alguns segundos para economizar bateria e dados. O Android decide o instante exato da cópia remota.",''')

rep('''                    Text("• O pedido de backup automático só é enviado quando houver conexão.")''', '''                    Text("• Alterações próximas são agrupadas em um único pedido de backup para economizar bateria e internet.")''')

p.write_text(s)

r = Path('README.md')
if r.exists():
    t = r.read_text()
    t = t.replace('## Recursos atuais — v0.7.1', '## Recursos atuais — v0.8.0')
    t = t.replace('## Recursos atuais — v0.7.0', '## Recursos atuais — v0.8.0')
    marker = '- Compras à vista e parceladas, com cada parcela posicionada na fatura correta.\n'
    if marker in t and 'Recorrências mensais' not in t:
        t = t.replace(marker, marker + '- Recorrências mensais com lançamentos futuros, exclusão isolada e cancelamento dos próximos meses.\n')
    r.write_text(t)

print('Applied v0.8.0 runtime optimization patch')
