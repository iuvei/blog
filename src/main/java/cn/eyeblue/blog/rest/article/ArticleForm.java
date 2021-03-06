package cn.eyeblue.blog.rest.article;

import cn.eyeblue.blog.config.AppContextManager;
import cn.eyeblue.blog.config.exception.BadRequestException;
import cn.eyeblue.blog.config.exception.UtilException;
import cn.eyeblue.blog.rest.base.BaseEntityForm;
import cn.eyeblue.blog.rest.core.FeatureType;
import cn.eyeblue.blog.rest.tag.Tag;
import cn.eyeblue.blog.rest.tag.TagService;
import cn.eyeblue.blog.rest.user.User;
import cn.eyeblue.blog.util.JsonUtil;
import cn.eyeblue.blog.util.StringUtil;
import cn.eyeblue.blog.util.ValidationUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.stream.Collectors;


@EqualsAndHashCode(callSuper = false)
@Data
public class ArticleForm extends BaseEntityForm<Article> {

    //标题
    @NotNull
    @Size(min = 1, max = 255, message = "名称必填并且最长255字")
    private String title;

    //路径
    @NotNull
    @Size(min = 1, max = 255, message = "路径必填并且最长255字")
    private String path;

    //标签
    private String tags;

    //封面图片
    private String posterTankUuid;

    //封面图片Url
    private String posterUrl;

    //摘要
    @Size(max = 500, message = "摘要必填并且最长500字")
    private String digest;

    //是否是markdown格式
    private Boolean isMarkdown = true;

    //内容
    @Size(max = 2147483646, message = "markdown最长2147483646字")
    private String markdown;

    //html
    @Size(max = 2147483646, message = "html必填并且最长2147483646字")
    private String html;

    //文章字数
    @NotNull
    private Integer words;

    //是否是私有文章
    private Boolean privacy = false;

    //是否置顶
    private Boolean top = false;

    //是否接受评论通知。
    private Boolean needNotify = true;

    //对应文档的uuid，只有类型是 DOCUMENT_ARTICLE时用到此字段
    private String documentUuid;

    private String puuid;

    //类型：知识库(DOCUMENT)，单篇文章(ARTICLE)，知识库中的文章(DOCUMENT_ARTICLE)
    @Enumerated(EnumType.STRING)
    private ArticleType type = ArticleType.ARTICLE;

    public ArticleForm() {
        super(Article.class);
    }

    @Override
    protected void update(Article article, User operator) {

        article.setTitle(title);

        article.setPath(path);
        article.validatePath();

        article.setPosterTankUuid(posterTankUuid);
        article.setPosterUrl(posterUrl);
        article.setDigest(digest);

        article.setIsMarkdown(isMarkdown);

        article.setMarkdown(markdown);
        article.setHtml(html);
        article.setWords(words);
        article.setPrivacy(privacy);

        //对于是否置顶，只有管理员有这个权限，其他人不可修改这一个选项
        if (operator.hasPermission(FeatureType.USER_MANAGE)) {
            article.setTop(top);
        }

        article.setNeedNotify(needNotify);

        article.setDocumentUuid(documentUuid);
        article.setPuuid(puuid);
        article.setType(type);

        //tag比较复杂，后面统一设置。
        if (StringUtil.isBlank(tags)) {
            tags = Article.EMPTY_JSON_ARRAY;
        }
        List<String> tagUuids = JsonUtil.toStringList(tags);
        TagService tagService = AppContextManager.getBean(TagService.class);

        List<Tag> tagList = tagService.checkTags(tagUuids, article.getUserUuid());
        List<String> okTags = tagList.stream().map(Tag::getUuid).collect(Collectors.toList());
        article.setTags(JsonUtil.toJson(okTags));

        //对于不同类型的article进行校验
        if (type == ArticleType.ARTICLE) {

            if (isMarkdown) {
                if (!ValidationUtil.checkParam(markdown)) {
                    throw new UtilException("markdown内容必填");
                }
            }

        } else if (type == ArticleType.DOCUMENT) {


        } else if (type == ArticleType.DOCUMENT_PLACEHOLDER_ARTICLE) {


        } else if (type == ArticleType.DOCUMENT_ARTICLE) {

            if (isMarkdown) {
                if (!ValidationUtil.checkParam(markdown)) {
                    throw new UtilException("markdown内容必填");
                }
            }

            if (StringUtil.isBlank(documentUuid)) {
                throw new BadRequestException("文档需要指定");
            }

            if (StringUtil.isBlank(puuid)) {
                throw new BadRequestException("父级菜单需要指定，第一级为ROOT");
            }

        } else if (type == ArticleType.DOCUMENT_BLANK) {

            //path要设为null
            article.setPath(null);

        } else if (type == ArticleType.DOCUMENT_URL) {

            //path要设为null
            article.setPath(null);
        }

    }

    public Article create(User operator) {

        //创建的时候设置好userUuid.
        Article entity = new Article();
        entity.setUserUuid(operator.getUuid());
        this.update(entity, operator);
        return entity;
    }


}

