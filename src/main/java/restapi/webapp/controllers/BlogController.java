package restapi.webapp.controllers;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import restapi.webapp.dto.BlogDTO;
import restapi.webapp.pojos.Blog;
import restapi.webapp.pojos.User;
import restapi.webapp.repos.BlogRepo;
import restapi.webapp.representationmodels.BlogDTOFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class BlogController {
    private final BlogRepo blogRepo;
    private final BlogDTOFactory blogDTOFactory;
    private final UserController userController;
    private final JokeController jokeController;

    public BlogController(BlogRepo blogRepo, BlogDTOFactory blogDTOFactory, UserController userController, JokeController jokeController) {
        this.blogRepo = blogRepo;
        this.blogDTOFactory = blogDTOFactory;
        this.userController = userController;
        this.jokeController = jokeController;
    }

    public Blog saveBlogToRepo(Blog blog){
        return blogRepo.save(blog);
    }

    public Optional<Blog> getBlog(Long id){
        return blogRepo.findById(id);
    }

    /**
     * for use when getting Collection of blogs contains certain joke.
     * @param blogs
     * @return info about all blog who contains a curtain joke
     */
    public ResponseEntity<?> blogsInfo(Collection<Blog> blogs) {
        return ResponseEntity.ok(
                blogDTOFactory.toCollectionModel(
                        blogs.stream()
                                .map(BlogDTO::new)
                                .collect(Collectors.toList()))
        );
    }

    /**
     * get collection model of the info about blogs whose title is the same as the param.
     * @param title
     * @return collection model of blog DTOs
     */
    @GetMapping("/blogs/{title}")
    public ResponseEntity<CollectionModel<EntityModel<BlogDTO>>> blogInfo(@PathVariable String title){
        return ResponseEntity.ok(
                blogDTOFactory.toCollectionModel(
                        blogRepo.findByTitle(title)
                                .stream()
                                .map(BlogDTO::new)
                                .collect(Collectors.toList())));
    }

    /**
     *
     * @return get info about all blogs in the repository.
     */
    @GetMapping("/blogs/info")
    public ResponseEntity<CollectionModel<EntityModel<BlogDTO>>> allBlogsInfo(){
        return ResponseEntity.ok(
                blogDTOFactory.toCollectionModel(
                        StreamSupport.stream(
                                blogRepo.findAll().spliterator(),
                                false)
                        .map(BlogDTO::new)
                        .collect(Collectors.toList())));
    }

    /**
     *
     * @param userName
     * @return if userName exist return list of all it's blog's
     * else return notFound status.
     */
    @GetMapping("/blogs/{userName}")
    public ResponseEntity<?> getBlogsByUser(@PathVariable String userName){
        Optional<User> userOptional = userController.getUserByUserName(userName);
        if(userOptional.isPresent()){
            return ResponseEntity.ok(
                    blogDTOFactory.toCollectionModel(
                    blogRepo.findByCreator(userOptional.get())
                            .stream().map(BlogDTO::new)
                            .collect(Collectors.toList())));
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/getBlog")
    public ResponseEntity<?> getBlogById(@RequestParam Long id){
        return blogRepo.findById(id).map(BlogDTO::new)
                .map(blogDTOFactory::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/addBlog")
    public ResponseEntity<?> addBlogToUser(@RequestParam() String userName, @RequestParam String blogTitle){
        Optional<User> userOptional = userController.getUserByUserName(userName);

        //TODO: change exception type
        User user = userOptional.orElseThrow(IllegalArgumentException::new);

        //if user don't have blog with this title
        if(!user.getStringBlogMap().containsKey(blogTitle))
        {
            Blog blog = new Blog(user, blogTitle);
            user.getStringBlogMap().put(blogTitle, blog);
            userController.saveUserToRepo(user);
            blogRepo.save(blog);
            return ResponseEntity.created(URI.create("http://localhost:8080/getBlog?id="+blog.getId()))
                    .body(blogDTOFactory.toModel(new BlogDTO(blog)));
        }else{
            //TODO: change bad request body.
            return ResponseEntity.badRequest().body("Error: User contains blog with the same Name.");
        }
    }

    @PutMapping("/blogs")
    public ResponseEntity<?>
    renameBlog(@RequestParam String userName, @RequestParam String oldName, @RequestParam String newName){

        Optional<User> userOptional = userController.getUserByUserName(userName);
        if(userOptional.isEmpty()){
            return ResponseEntity.badRequest().body("Error: User Name Invalid");
        }

        User user = userOptional.get();
        if(!user.getStringBlogMap().containsKey(oldName)){
            return ResponseEntity.badRequest().body("Error: Blog title does not exist");
        }

        Blog blog = user.getStringBlogMap().get(oldName);
        blog.setTitle(newName);

        user.getStringBlogMap().remove(oldName);
        user.getStringBlogMap().put(newName,blog);

        userController.saveUserToRepo(user);
        blogRepo.save(blog);

        return ResponseEntity.ok(blogDTOFactory.toModel(new BlogDTO(blog)));
    }

    @DeleteMapping("/blog")
    public ResponseEntity<?> deleteBlogFromUser(@RequestParam String userName,@RequestParam String blogTitle){
        Optional<User> userOptional = userController.getUserByUserName(userName);
        if(userOptional.isEmpty()){
            return ResponseEntity.badRequest().body("Error: User Name Invalid");
        }

        User user = userOptional.get();
        if(!user.getStringBlogMap().containsKey(blogTitle)){
            return ResponseEntity.badRequest().body("Error: Blog title does not exist");
        }

        Blog blog = user.getStringBlogMap().get(blogTitle);
        user.getStringBlogMap().remove(blogTitle);

        userController.saveUserToRepo(user);
        blogRepo.delete(blog);

        return ResponseEntity.ok().body(userController.getUserModel(user));
    }




}