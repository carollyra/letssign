# LetsSign

Aplicativo Android nativo para envio e assinatura de documentos PDF. Um Admin
monta o documento, escolhe quem assina e onde; cada Signatário abre o documento
no app, desenha a assinatura e ela é gravada no PDF.

Esta é a Sprint 1: não há backend. Tudo fica no aparelho, e Admin e Signatários
usam o mesmo dispositivo, trocando de perfil por logout e login.

## Como executar

Requisitos:

- JDK 17 ou mais novo
- Android SDK com a plataforma 37.0 (`platforms;android-37.0`)
- Emulador ou aparelho com Android 8.0 (API 26) ou superior

```bash
./gradlew installDebug          # compila e instala no aparelho conectado
./gradlew assembleDebug         # só gera o APK em app/build/outputs/apk/debug
./gradlew testDebugUnitTest     # testes unitários (JVM + Robolectric)
./gradlew connectedDebugAndroidTest   # testes de PDF, precisam de aparelho ou emulador
```

Os testes instrumentados desinstalam o app ao terminar, então os dados
cadastrados no aparelho são apagados.

Também dá para abrir a pasta no Android Studio e rodar a configuração `app`.

## Credenciais do Admin padrão

Criado na primeira execução:

| E-mail | Senha |
|---|---|
| `admin@letssign.com.br` | `Admin@123` |

Os Signatários são cadastrados pelo Admin, que define o e-mail e a senha de
cada um.

## Roteiro de uso

1. Entre como Admin e cadastre os signatários em **Signatários**.
2. Em **Novo documento**: informe o título e escolha o PDF; marque quem assina
   e o papel de cada um; posicione os campos de assinatura sobre as páginas;
   revise e envie.
3. Saia e entre como um Signatário. O documento aparece em **Pendentes**: abra,
   toque em **Assinar documento**, desenhe e confirme.
4. Repita para os demais signatários.
5. Volte como Admin. O documento passa por *Enviado*, *Parcialmente assinado* e
   *Concluído*; o acompanhamento mostra quem assinou, o histórico e o PDF final.

## Arquitetura

MVVM com casos de uso, em três camadas dentro do módulo `app`
(`br.com.letssign`):

```
domain/   model · repository (interfaces) · envio · regra · usecase
data/     local (Room) · pdf · seguranca · sessao · envio · repository · di
ui/       navigation · theme · common · pdf · login · home · signatarios
          documento · campos · revisao · assinatura · acompanhamento
di/       qualificadores de dispatchers e escopo da aplicação
```

`domain` é Kotlin puro. As telas e os casos de uso só conhecem as interfaces
dele: `AutenticacaoRepository`, `SignatarioRepository`, `DocumentoRepository`,
`ArmazenamentoPdf`, `AssinadorPdf` e `EnvioGateway`. As implementações locais
ficam em `data` e são ligadas pelo Hilt em `data/di`. Trocar a persistência por
APIs REST significa escrever novas implementações dessas interfaces e mudar os
`@Binds`, sem tocar em `domain` nem em `ui`.

### Decisões

**Isolamento de dados nas consultas.** Toda leitura e escrita de documento
recebe quem está pedindo. As queries do Room filtram por `adminId` ou por
`signatarioId`, e as do signatário excluem rascunhos. Documento de outro
usuário se comporta como inexistente, mesmo que alguém chame o repositório
direto.

**Campos em frações da página.** Página, posição e tamanho de cada campo são
gravados como frações de 0 a 1 da página exibida, com origem no canto superior
esquerdo. Pixel de tela só existe dentro do Compose, então o mesmo documento
abre igual em qualquer aparelho. O arraste e o redimensionamento passam por
`GeometriaCampo`, que mantém o campo dentro da página.

**Conversão para o PDF.** `ConversaoPdf` leva a área do campo para o espaço do
PDF considerando a origem no canto inferior esquerdo, a caixa de recorte e a
rotação da página (0°, 90°, 180°, 270°), e gera a matriz que desenha a
assinatura de pé. A imagem é encaixada no campo sem distorcer. Além dos testes
unitários da matemática, há um teste instrumentado que assina PDFs nas quatro
rotações, renderiza o resultado e confere os pixels.

**Duas bibliotecas de PDF.** `PdfRenderer` exibe as páginas e o PDFBox-Android
grava as assinaturas. Na importação o arquivo precisa abrir nas duas; PDF
inválido, corrompido, com senha ou com restrição de edição é recusado com uma
mensagem específica, e o usuário pode escolher outro. O limite é de 50 MB.

**Arquivos dentro do app.** O PDF escolhido é copiado para `filesDir/documentos`
e o banco guarda só nome, caminhos, datas e situação. Cada assinatura gera um
arquivo novo: `caminhoOriginal` aponta sempre para o PDF enviado pelo Admin e
`caminhoAtual` para a versão com as assinaturas coletadas até o momento.

**Imutabilidade depois do envio.** As operações de edição só aceitam rascunho e
essa checagem acontece dentro da transação do repositório. Para alterar um
documento enviado existe "criar nova versão": um rascunho novo, com versão + 1,
ligado ao original e com os mesmos signatários e campos, que segue o fluxo
desde o começo.

**Envio idempotente.** O disparo dos convites fica atrás de `EnvioGateway`, que
hoje não faz nada porque o envio é local. O registro do envio é uma única
transação que só age sobre rascunho, então repetir a chamada não muda nada nem
duplica evento. Cada tentativa leva a mesma chave de idempotência
(`documento-<id>-v<versão>`) e o vínculo documento + signatário tem índice
único. Se o disparo falhar, o documento continua rascunho e a tela oferece
"Tentar novamente".

**Situação do documento.** `SituacaoDocumento` calcula a situação a partir das
assinaturas: parcialmente assinado na primeira, concluído quando todos os
obrigatórios assinaram. O vínculo tem o campo `obrigatorio`, com padrão `true`.

**Histórico.** A tabela `eventos_documento` registra criação, envio, cada
assinatura, conclusão e criação de nova versão, com usuário e data e hora. O
acompanhamento lê dela.

**Senhas.** PBKDF2-HMAC-SHA256 com 150 mil iterações e salt aleatório de 16
bytes por usuário. O banco guarda só o hash e o salt, e a comparação é feita em
tempo constante. A sessão guarda apenas o id do usuário, no DataStore.

**Backup desligado.** Os dados do app ficam fora do backup em nuvem e da
transferência entre aparelhos, porque incluem os documentos.

### Versões

AGP 9.5.1 com o Kotlin embutido fixado em 2.4.21, KSP 2.3.13, Gradle 9.8.2,
Compose BOM 2026.09.00, Room 2.8.5, Hilt 2.60.1, Navigation 2.10.2 e
PDFBox-Android 2.0.27.0. `compileSdk` 37, `targetSdk` 36, `minSdk` 26.

## Testes

- **Unitários** (`app/src/test`): validações de campos e de envio, geometria dos
  campos, conversão de coordenadas, transição de situação, PBKDF2, e os casos de
  uso rodando sobre os repositórios reais com Room em memória (login,
  signatários, rascunho, envio com falha e nova tentativa, idempotência,
  imutabilidade, assinatura, nova versão, histórico e isolamento entre
  usuários).
- **Instrumentados** (`app/src/androidTest`): importação de PDF válido, falso,
  vazio, truncado e protegido; gravação da assinatura em páginas com rotação e
  recorte, com o arquivo original preservado.

## Fora do escopo desta sprint

- Backend, API REST e sincronização entre aparelhos.
- Envio real de convite por e-mail, SMS ou notificação.
- Assinatura digital com certificado (ICP-Brasil): a assinatura é a imagem
  desenhada, gravada no PDF.
- Exportar ou compartilhar o PDF final para fora do app; ele é aberto no
  visualizador interno.
- Tela para marcar um signatário como opcional. O modelo e as regras já tratam
  o campo `obrigatorio`, mas todo signatário nasce obrigatório.
- Exclusão de rascunhos e de signatários.
- Zoom nas páginas e layout próprio para tablet.
- Cadastro de outros Admins, troca de senha do Admin e recuperação de senha.
- Criptografia dos arquivos em repouso e migrações do banco (o schema está na
  versão 1).
- O desenho da assinatura em andamento não sobrevive à rotação da tela.
