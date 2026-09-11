# Meu Cartão

Aplicativo Android offline-first para controle de compras e faturas de cartão de crédito.

## Recursos atuais — v0.8.0

- Registro rápido de valor, data, categoria e descrição opcional.
- Compras à vista e parceladas, com cada parcela posicionada na fatura correta.
- Recorrências mensais com lançamentos futuros, exclusão isolada e cancelamento dos próximos meses.
- Faturas separadas por cartão e histórico preservado por mês.
- Edição do valor de um lançamento diretamente na fatura.
- Ações de edição e exclusão ainda mais compactas na mesma linha do valor, com lápis discreto e lixeira vermelha vetorial.
- Exclusão com confirmação antes de apagar.
- Categorias prontas, incluindo Mercado, Padaria, Lanches, Sorvetes, Restaurante, Combustível, Saúde e outras; também permite categorias personalizadas.
- Análises por categoria e gráficos comparativos com linha de tendência.
- Funcionamento offline: os dados continuam sendo gravados localmente sem depender da internet.
- Dados locais criptografados com AES-GCM e chave protegida pelo Android Keystore.
- Backup opcional na Conta Google usando a pasta privada `appData` do Google Drive.
- Fluxo de autorização Google corrigido para validar o retorno real da autorização antes de classificá-lo como cancelamento e para exibir erros de OAuth/configuração de forma explícita.
- Quando o backup está ativado, novas compras, edições, exclusões e mudanças de configuração entram na fila de sincronização e são enviadas quando houver internet.
- Ao reinstalar o aplicativo ou trocar de aparelho, a mesma Conta Google pode restaurar o backup existente depois que a autorização Google estiver configurada para a assinatura do aplicativo.
- GitHub Actions para gerar automaticamente um APK de teste.

## APK de teste

Abra a aba **Actions** do repositório, selecione a execução mais recente de **Build Android APK** e baixe o artefato mais recente. Dentro dele estará o arquivo `app-debug.apk`.

> A versão `debug` serve para testes. Para publicar na Google Play, gere um Android App Bundle (`.aab`) de release assinado. A chave de assinatura nunca deve ser colocada no repositório público.
