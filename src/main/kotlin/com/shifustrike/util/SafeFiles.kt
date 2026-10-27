package com.shifustrike.util

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object SafeFiles {
    /** Grava em um .tmp e troca de uma vez, para não corromper o arquivo se o servidor cair no meio. */
    fun writeAtomic(target: Path, content: String) = writeAtomic(target) { Files.writeString(it, content) }

    /** Mesma troca atômica, para quem escreve o .tmp por conta própria (NBT, binário). */
    fun writeAtomic(target: Path, write: (Path) -> Unit) {
        Files.createDirectories(target.parent)
        val tmp = target.resolveSibling(target.fileName.toString() + ".tmp")
        write(tmp)
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
