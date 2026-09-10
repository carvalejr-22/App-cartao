# Configuração do backup Google Drive

O aplicativo usa o escopo restrito ao diretório privado `appDataFolder` do Google Drive. O usuário autoriza explicitamente o acesso e os dados do app não ficam visíveis no Meu Drive.

## 1. Criar/configurar projeto no Google Cloud

1. Acesse o Google Cloud Console e crie ou selecione o projeto do aplicativo.
2. Em **APIs e serviços > Biblioteca**, habilite **Google Drive API**.
3. Configure a tela de consentimento OAuth com nome e dados públicos do aplicativo.
4. Em **Credenciais**, crie um **ID do cliente OAuth > Android**.
5. Use o package name:

   `com.carlos.appcartao`

6. Informe o SHA-1 do certificado da versão que será testada/publicada.

## 2. Assinatura na Google Play

Quando o app estiver no Play Console com **Play App Signing** habilitado, abra a área de integridade/assinatura do aplicativo e copie o SHA-1 do certificado de assinatura do app. Cadastre esse SHA-1 no cliente OAuth Android do Google Cloud.

Se quiser testar uma versão assinada fora da Play Store, também cadastre o SHA-1 da chave usada nessa versão.

## 3. Como a sincronização funciona

- O arquivo local continua sendo a fonte de trabalho do aplicativo e permanece criptografado com AES-GCM/Android Keystore.
- Cada alteração relevante atualiza um carimbo local e agenda sincronização via WorkManager.
- A sincronização só roda quando há rede.
- O backup remoto é armazenado em `appDataFolder` como `meu_cartao_backup_v1.json`.
- Na inicialização, se já houver autorização Google válida, o app compara o backup remoto com os dados locais e restaura o mais recente.
- O botão **Sincronizar agora** força uma tentativa imediata após autorização.
- O botão **Desconectar backup** revoga o acesso solicitado pelo aplicativo e interrompe novas sincronizações no aparelho.

## 4. Antes da produção

- Validar login/autorização com uma conta de teste.
- Testar: criar compra offline, voltar à internet e confirmar sincronização.
- Testar: editar e excluir uma compra e confirmar que o backup é atualizado.
- Testar: instalar em outro aparelho/perfil de teste, conectar a mesma Conta Google e confirmar restauração.
- Gerar e testar um `.aab` de release assinado antes de enviar à Google Play.
- Preencher corretamente a seção **Segurança dos dados** do Play Console, incluindo o uso do Google Drive para backup solicitado pelo usuário.

## Observação de conflito

A v0.6.0 usa uma estratégia simples de snapshot com “versão mais recente vence”, adequada para uso pessoal em um aparelho por vez e restauração. Antes de oferecer edição simultânea em vários aparelhos, a sincronização deve evoluir para registros individuais com resolução de conflito por item.
