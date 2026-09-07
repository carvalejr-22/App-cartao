# App Cartão

Aplicativo Android offline para controle de compras no cartão de crédito.

## Recursos da primeira versão

- Registro rápido de valor, data, categoria e descrição opcional.
- Data de hoje selecionada por padrão e opção de escolher outra data.
- Fatura atual calculada pelo dia configurado de início do ciclo.
- Cadastro do melhor dia para compra / virada.
- Histórico da fatura atual com exclusão de lançamentos.
- Análise por categoria, ticket médio e gráfico comparativo das últimas faturas.
- Armazenamento somente no aparelho, sem permissão de internet.
- Dados criptografados com AES-GCM e chave protegida pelo Android Keystore.
- GitHub Actions para gerar automaticamente um APK de teste.

## APK

Abra a aba **Actions** do repositório, selecione a execução mais recente de **Build Android APK** e baixe o artefato **App-cartao-debug**. Dentro dele estará o arquivo `app-debug.apk`.

> A versão `debug` serve para testes pessoais. Uma versão de distribuição definitiva deve ser assinada com uma chave de release mantida fora do repositório.
