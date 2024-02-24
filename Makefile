SRC = $(wildcard *.java)

run: main.class
	@java Main

main.class: $(SRC)
	javac $(SRC)
