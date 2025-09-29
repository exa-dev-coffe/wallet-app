# Creating a New Module

To create a new module in the project, you can use the `ScaffoldGenerator` tool provided in the `tools/scaffold`
directory. This tool automates the process of setting up the basic structure and files needed for a new module.    
Run the following command in your terminal:

```java
java tools/scaffold/ScaffoldGenerator.java<module-name>
```

Replace `<module-name>` with the desired name for your new module. This command will generate the necessary files and
directory structure for the module.

After running the command, you should see a new directory created under the `modules` folder with the name you
provided. Inside this directory, you will find the following files:

- `<ModuleName>Model.java`:  The main model class for the module.
- `<ModuleName>Route.java`:  The routing class for handling requests related to the module.
- `<ModuleName>Repository.java`:  The repository class for data access and storage.
- `<ModuleName>Service.java`:  The service class containing the business logic for the module.

Make sure to replace `<ModuleName>` with the actual name of your module in PascalCase format. For example, if your
module name is `user-profile`, the files will be named `UserProfileModel.java`, `UserProfileRoute.java`,
`UserProfileRepository.java`, and `UserProfileService.java`.    



