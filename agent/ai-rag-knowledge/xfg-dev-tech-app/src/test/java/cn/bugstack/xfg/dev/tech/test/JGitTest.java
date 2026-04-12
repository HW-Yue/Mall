package cn.bugstack.xfg.dev.tech.test;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class JGitTest {

    @Resource
    private OllamaChatClient ollamaChatClient;
    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private SimpleVectorStore simpleVectorStore;
    @Resource
    private PgVectorStore pgVectorStore;

    @Test
    public void test() throws Exception {
        String repoURL = "https://gitcode.com/m0_73955445/group-buy-market-xingsheng.git";
        String username = "gcw_26Qwt3KW";
        String password = "AeVmxtpj2bjNGURzWxp1Ka9v";

        String localPath = "./cloned-repo";
        log.info("克隆路径：" + new File(localPath).getAbsolutePath());

        FileUtils.deleteDirectory(new File(localPath));

        Git git = Git.cloneRepository()
                .setURI(repoURL)
                .setDirectory(new File(localPath))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                .call();

        git.close();
    }

    @Test
    public void test_file() throws IOException {
        // 建议使用绝对路径或确保相对路径正确
        Path rootPath = Paths.get("./cloned-repo");

        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // 💡 关键：直接跳过整个 .git 目录及其子目录
                if (dir.getFileName() != null && dir.getFileName().toString().equals(".git")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // 过滤掉隐藏文件和空文件
                if (attrs.isRegularFile() && attrs.size() > 0 && !file.getFileName().toString().startsWith(".")) {
                    log.info("正在处理文件: {}", file.toString());

                    try {
                        PathResource resource = new PathResource(file);
                        TikaDocumentReader reader = new TikaDocumentReader(resource);

                        List<Document> documents = reader.get();
                        // 💡 注意：reader.get() 有可能返回空列表，建议加个判断
                        if (documents.isEmpty()) return FileVisitResult.CONTINUE;

                        // 拆分文档
                        List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

                        // 统一添加元数据
                        for (Document doc : documentSplitterList) {
                            doc.getMetadata().put("knowledge", "group-buy-market-liergou");
                            // 建议把文件名也存入元数据，方便追溯源文件
                            doc.getMetadata().put("source", file.getFileName().toString());
                        }

                        // 存入向量库
                        pgVectorStore.accept(documentSplitterList);

                    } catch (Exception e) {
                        log.error("文件处理失败: {}", file, e);
                        // 单个文件失败可以选择继续遍历
                    }
                }
                // 💡 修正：必须返回 CONTINUE，否则遍历会中断
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
