all: help_page docs

help_page: css/pandoc.css help.md.in
	rm ../alpine-term-app/app/src/main/assets/help/help.html
	pandoc --self-contained --css css/pandoc.css --from=markdown --to=html --output help.html help.md.in
	mv help.html ../alpine-term-app/app/src/main/assets/help/help.html

docs: doc
	rm -r ../alpine-term-app/app/src/main/assets/help/doc
	cp -a ./doc ../alpine-term-app/app/src/main/assets/help/doc
