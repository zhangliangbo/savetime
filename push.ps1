git add .
git commit -m ("auto push "+(Get-Date -Format "yyyyMMdd"))
git pull --ff-only
git push
pause