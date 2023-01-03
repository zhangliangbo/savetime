git add .
git commit -m ("auto push "+(Get-Date -Format "yyyyMMdd"))
git pull --rebase
git push