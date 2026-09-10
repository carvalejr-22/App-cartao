# Meu Cartão

Aplicativo Android offline-first para controle de compras e faturas de cartão de crédito.

## Recursos atuais — v0.6.1

- Registro rápido de valor, data, categoria e descrição opcional.
- Compras à vista e parceladas, com cada parcela posicionada na fatura correta.
- Faturas separadas por cartão e histórico preservado por mês.
- Edição do valor de um lançamento diretamente na fatura.
- Ações de edição e exclusão compactas na mesma linha do valor, com lápis discreto e lixeira vermelha vetorial.
- Exclusão com confirmação antes de apagar.
- Categorias prontas, incluindo Mercado, Padaria, Lanches, Sorvetes, Restaurante, Combustível, Saúde e outras; também permite categorias personalizadas.
- Análises por categoria e gráficos comparativos com linha de tendência.
- Funcionamento offline: os dados continuam sendo gravados localmente sem depender da internet.
- Dados locais criptografados com AES-GCM e chave protegida pelo Android Keystore.
- Backup opcional na Conta Google usando a pasta privada `appData` do Google Drive.
- Quando o backup está ativado, novas compras, edições, exclusões e mudanças de configuração entram na fila de sincronização e são enviadas quando houver internet.
- Ao reinstalar o aplicativo ou trocar de aparelho, a mesma Conta Google pode restaurar o backup existente.
- GitHub Actions para gerar automaticamente um APK de teste.

## APK de teste

Abra a aba **Actions** do repositório, selecione a execução mais recente de **Build Android APK** e baixe o artefato mais recente. Dentro dele estará o arquivo `app-debug.apk`.

> A versão `debug` serve para testes. Para publicar na Google Play, gere um Android App Bundle (`.aab`) de release assinado. A chave de assinatura nunca deve ser colocada no repositório público.

## Backup Google Drive

O código da sincronização já faz parte do aplicativo. Para a autorização funcionar numa versão distribuída, ainda é necessário configurar o projeto no Google Cloud Console: habilitar a Google Drive API e cadastrar o cliente OAuth Android para o pacote `com.carlos.appcartao` com a impressão SHA-1 do certificado usado na assinatura do app. Veja `GOOGLE_DRIVE_SETUP.md`.
