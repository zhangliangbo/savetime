git add .
git commit -m ("auto push "+(Get-Date -Format "yyyyMMddHHmmss"))
git pull --ff-only
git push
pause