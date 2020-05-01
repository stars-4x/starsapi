@echo off
echo https://help.github.com/articles/fork-a-repo/
echo git remote add upstream ....
echo Sync help. Type:
echo git fetch upstream
echo git checkout master
echo git merge upstream/master
echo commit changes..
echo git push origin/master
echo.
echo In this case:
echo git push origin
echo git push upstream
echo.
git remote -v
