# First you will need to get the repository on your machine locally:
I'd highly recommend using Visual Studio Code, https://code.visualstudio.com/Download, as we can all be using the same environment.

You will also need to be able to run some version of Java, I currently am using Java 11 but would be happy to use a more recent version, just reach out

To add the repository locally, you will clone it from the main repository page on github. To do this, click on the green "<> Code" button located on the repository main page and copy the HTTPS web URL.
In a Visual Studio Terminal first navigate to the directory you would like to store the project.
Then type this command in the terminal:
```
"git clone 'link'"
```
## Visual Studio may need to go through multiple steps to connect to github:
There is documentation (from github) that will detail how you can connect your github account if this command fails. If you need help troubleshooting please reach out, I already had my account connected from previous classes and do not know all of the steps it will require to connect the accounts.

## How we should manage branching:
We should not be pushing our code directly onto the github repository and instead use branches.
In order to work on a branch you will write the following command in the project directory:
```
git checkout -b 'branchName' origin/main
```
To name the branch, I'd recommend we use our names, so and example would be "Mathew's Branch" however make sure it is easy to remember and type.
While using your branch, I highly recommend that before you start making changing to code that you perform:
```
git pull origin main
```
This will update your local code to be up to date with the github repo.
Finally, when you want to push your changes to main use:
```
git push origin 'branchName'
```
Note that to push changes you must first stage the changes with:
```
git add 'filename'
```
or for all files
```
git add .
```
Then commit the changes with a message like so:
```
git commit -m "some commit message"
```


