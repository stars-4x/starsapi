@echo off
echo https://help.github.com/articles/fork-a-repo/
echo git remote add upstream ....
git remote -v
echo Sync help. Type:
echo git fetch upstream
echo git checkout master
echo git merge upstream/master
echo commit changes..
echo git push origin/master